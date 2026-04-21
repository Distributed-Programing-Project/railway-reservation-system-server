package vn.edu.iuh.fit.server.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.constant.InvoiceType;
import vn.edu.iuh.fit.server.constant.TicketStatus;
import vn.edu.iuh.fit.server.dto.ExchangeTicketRequestDTO;
import vn.edu.iuh.fit.server.model.Invoice;
import vn.edu.iuh.fit.server.model.InvoiceDetail;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Ticket;
import vn.edu.iuh.fit.server.repository.ScheduleDetailRepository;
import vn.edu.iuh.fit.server.repository.TicketRepository;
import vn.edu.iuh.fit.server.repository.impl.ScheduleDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.TicketRepositoryImpl;
import vn.edu.iuh.fit.server.service.TicketService;
import vn.edu.iuh.fit.server.util.JPAUtils;
import vn.edu.iuh.fit.server.util.ValidationUtils;

public class TicketServiceImpl implements TicketService {
  private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);
  private static final double EXCHANGE_FEE = 50000.0;

  // Khai báo các Repository theo đúng Interface
  private final TicketRepository ticketRepository = new TicketRepositoryImpl();
  private final ScheduleDetailRepository scheduleDetailRepository = new ScheduleDetailRepositoryImpl();

  @Override
  public Response exchangeTickets(ExchangeTicketRequestDTO requestDTO) {
    // 1. Validate dữ liệu đầu vào (JSR-303)
    List<String> errors = ValidationUtils.validate(requestDTO);
    if (!errors.isEmpty()) {
      return Response.error("Dữ liệu không hợp lệ: " + String.join(", ", errors));
    }
    if (requestDTO.getOldTicketIds().size() != requestDTO.getNewScheduleDetailIds().size()) {
      return Response.error("Số lượng vé cũ và ghế mới phải khớp nhau.");
    }

    // 2. Quản lý Transaction tại tầng Service
    EntityManager em = JPAUtils.getEntityManager();
    EntityTransaction transaction = em.getTransaction();

    try {
      transaction.begin();

      // 3. Lấy danh sách vé cũ (Sử dụng JOIN FETCH trong Repo để tránh N+1)
      List<Ticket> oldTickets = ticketRepository.findTicketsForExchange(requestDTO.getOldTicketIds(), em);
      if (oldTickets.size() != requestDTO.getOldTicketIds().size()) {
        return Response.error("Một số vé không tồn tại hoặc không hợp lệ.");
      }

      // 4. Kiểm tra các quy tắc nghiệp vụ (24h, Trạng thái PAID, Đã đổi chưa)
      Response validationRes = validateBusinessRules(oldTickets);
      if (validationRes != null)
        return validationRes;

      double totalOldPrice = 0;
      double totalNewPrice = 0;
      List<Ticket> newTickets = new ArrayList<>();

      // 5. Giải phóng ghế từ vé cũ
      for (Ticket oldTicket : oldTickets) {
        totalOldPrice += oldTicket.getScheduleDetail().getPriceSeat().doubleValue();

        // Cập nhật trạng thái vé cũ sang EXCHANGED
        oldTicket.setExchanged(true);
        oldTicket.setStatus(TicketStatus.EXCHANGED);
        em.merge(oldTicket);
      }

      // 6. Xử lý đặt chỗ mới và tạo vé mới
      for (int i = 0; i < requestDTO.getNewScheduleDetailIds().size(); i++) {
        String newSeatId = requestDTO.getNewScheduleDetailIds().get(i);

        // Tìm thông tin lịch trình ghế mới qua Repo
        ScheduleDetail newSeat = scheduleDetailRepository.findById(newSeatId, em);
        if (newSeat == null) {
          throw new IllegalArgumentException("Không tìm thấy lịch trình ghế: " + newSeatId);
        }

        // Kiểm tra ghế trống dựa trên logic của Leader (getSoldSeatIds)
        String scheduleId = newSeat.getSchedule().getId();
        Set<String> soldSeatIds = scheduleDetailRepository.getSoldSeatIds(em, scheduleId);

        if (soldSeatIds.contains(newSeat.getSeat().getId())) {
          return Response.error("Ghế số " + newSeat.getSeat().getNumber() + " đã có người đặt.");
        }

        totalNewPrice += newSeat.getPriceSeat().doubleValue();

        // Merge để đảm bảo Optimistic Locking hoạt động
        em.merge(newSeat);

        // Build vé mới
        Ticket newTicket = Ticket.builder()
            .customer(oldTickets.get(i).getCustomer())
            .scheduleDetail(newSeat)
            .type(oldTickets.get(i).getType()) // Giữ nguyên đối tượng (Người lớn/Trẻ em)
            .roundTrip(oldTickets.get(i).isRoundTrip())
            .originalTicketId(oldTickets.get(i).getId()) // Lưu vết vé cũ
            .status(TicketStatus.PAID)
            .exchanged(false)
            .build();

        // Lưu vé mới qua Repo
        boolean isCreated = ticketRepository.createTicket(newTicket, em);
        if (!isCreated) {
          return Response.error("Lỗi hệ thống: Không thể tạo vé mới.");
        }

        newTickets.add(newTicket);
      }

      // 7. Tạo Hóa đơn đổi vé (Invoice EXCHANGE)
      double totalFee = oldTickets.size() * EXCHANGE_FEE;
      double finalAmount = totalFee + (totalNewPrice - totalOldPrice);

      Invoice invoice = Invoice.builder()
          .issueDate(LocalDateTime.now())
          .type(InvoiceType.EXCHANGE)
          .totalAmount(finalAmount)
          .customer(oldTickets.get(0).getCustomer()) // Gán đại diện khách hàng đầu tiên
          .taxCode(requestDTO.getTaxCode())
          .companyName(requestDTO.getCompanyName())
          .build();
      em.persist(invoice);

      // 8. Tạo chi tiết hóa đơn (InvoiceDetail) gắn với các vé mới
      double subTotalPerTicket = finalAmount / newTickets.size();
      for (Ticket newTicket : newTickets) {
        InvoiceDetail detail = InvoiceDetail.builder()
            .invoice(invoice)
            .ticket(newTicket)
            .subTotal(subTotalPerTicket)
            .isReturned(false)
            .build();
        em.persist(detail);
      }

      // 9. Chốt giao dịch
      transaction.commit();
      log.info("Giao dịch đổi vé hoàn tất. Số lượng: {}", oldTickets.size());
      return Response.success("Đổi vé thành công. Số tiền thanh toán: " + finalAmount, null);

    } catch (OptimisticLockException e) {
      rollbackQuietly(transaction);
      log.warn("Xung đột dữ liệu (Race condition) khi chiếm ghế.");
      return Response.error("Ghế bạn chọn vừa có người khác đặt nhanh hơn. Vui lòng thử lại!");
    } catch (Exception e) {
      rollbackQuietly(transaction);
      log.error("Lỗi nghiệp vụ đổi vé: ", e);
      return Response.error("Lỗi hệ thống: " + e.getMessage());
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  /**
   * Kiểm tra các ràng buộc: Đổi 1 lần, trạng thái PAID và quy tắc 24h
   */
  private Response validateBusinessRules(List<Ticket> oldTickets) {
    LocalDateTime now = LocalDateTime.now();
    for (Ticket t : oldTickets) {
      // Rule 1: Mỗi vé chỉ được đổi 1 lần
      if (t.isExchanged() || t.getOriginalTicketId() != null) {
        return Response.error("Vé " + t.getId() + " đã từng được đổi trước đó.");
      }
      // Rule 2: Chỉ đổi vé đã thanh toán (PAID)
      if (t.getStatus() != TicketStatus.PAID) {
        return Response.error("Vé " + t.getId() + " không ở trạng thái hợp lệ để đổi.");
      }
      // Rule 3: Cách giờ khởi hành ít nhất 24h
      LocalDateTime departureTime = t.getScheduleDetail().getSchedule().getDepartureTime();
      if (departureTime != null) {
        long hoursRemaining = ChronoUnit.HOURS.between(now, departureTime);
        if (hoursRemaining < 24) {
          return Response
              .error("Vé " + t.getId() + " không được đổi vì chỉ còn " + hoursRemaining + "h tới giờ khởi hành.");
        }
      }
    }
    return null;
  }

  private void rollbackQuietly(EntityTransaction transaction) {
    if (transaction != null && transaction.isActive()) {
      transaction.rollback();
    }
  }
}