package vn.edu.iuh.fit.server.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.constant.InvoiceType;
import vn.edu.iuh.fit.server.constant.TicketStatus;
import vn.edu.iuh.fit.server.dto.ExchangeTicketRequestDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketConfirmDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketPreviewDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketPreviewRequestDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketSearchDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketTicketDTO;

import vn.edu.iuh.fit.server.model.Invoice;
import vn.edu.iuh.fit.server.model.InvoiceDetail;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Ticket;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.InvoiceDetailRepository;
import vn.edu.iuh.fit.server.repository.InvoiceRepository;
import vn.edu.iuh.fit.server.repository.ScheduleDetailRepository;
import vn.edu.iuh.fit.server.repository.TicketRepository;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.InvoiceDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.InvoiceRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.TicketRepositoryImpl;
import vn.edu.iuh.fit.server.service.TicketService;
import vn.edu.iuh.fit.server.util.JPAUtils;
import vn.edu.iuh.fit.server.util.ValidationUtils;

public class TicketServiceImpl implements TicketService {
  private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);
  private static final double EXCHANGE_FEE = 50000.0;
  private static final double MIN_RETURN_FEE_PER_TICKET = 10_000.0;
  private static final long MINUTES_4H = 4 * 60;
  private static final long MINUTES_24H = 24 * 60;
  private static final String INVALID_QR_CODE = "INVALID";
  private static final double REFUND_TOLERANCE = 1.0;

  // Khai báo các Repository theo đúng Interface
  private final TicketRepository ticketRepository = new TicketRepositoryImpl();
  private final ScheduleDetailRepository scheduleDetailRepository = new ScheduleDetailRepositoryImpl();
  private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();
  private final InvoiceRepository invoiceRepository = new InvoiceRepositoryImpl();
  private final InvoiceDetailRepository invoiceDetailRepository = new InvoiceDetailRepositoryImpl();

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

      // Map lại danh sách vé cũ theo ID để đảm bảo lấy đúng thứ tự mapping với ghế mới
      Map<String, Ticket> ticketMap = oldTickets.stream()
          .collect(Collectors.toMap(Ticket::getId, t -> t));

      double totalOldPrice = 0;
      double totalNewPrice = 0;
      List<Ticket> newTickets = new ArrayList<>();

      // Cache danh sách ghế đã bán theo lịch trình để tránh N+1 Query
      Map<String, Set<String>> soldSeatIdsMap = new HashMap<>();

      // 5. Giải phóng ghế từ vé cũ
      for (Ticket oldTicket : oldTickets) {
        totalOldPrice += oldTicket.getScheduleDetail().getPriceSeat().doubleValue();

        // Cập nhật trạng thái vé cũ sang EXCHANGED
        oldTicket.setExchanged(true);
        oldTicket.setStatus(TicketStatus.EXCHANGED);
        ticketRepository.updateTicket(em, oldTicket);
      }

      // 6. Xử lý đặt chỗ mới và tạo vé mới
      for (int i = 0; i < requestDTO.getNewScheduleDetailIds().size(); i++) {
        String oldTicketId = requestDTO.getOldTicketIds().get(i);
        String newSeatId = requestDTO.getNewScheduleDetailIds().get(i);

        Ticket oldTicket = ticketMap.get(oldTicketId);

        // Tìm thông tin lịch trình ghế mới qua Repo
        ScheduleDetail newSeat = scheduleDetailRepository.findById(newSeatId, em);
        if (newSeat == null) {
          throw new IllegalArgumentException("Không tìm thấy lịch trình ghế: " + newSeatId);
        }

        // Kiểm tra ghế trống (Sử dụng Cache để tránh lặp lại query cùng một chuyến tàu)
        String scheduleId = newSeat.getSchedule().getId();
        Set<String> soldSeatIds = soldSeatIdsMap.computeIfAbsent(scheduleId,
            id -> scheduleDetailRepository.getSoldSeatIds(em, id));

        if (soldSeatIds.contains(newSeat.getSeat().getId())) {
          return Response.error("Ghế số " + newSeat.getSeat().getNumber() + " của chuyến "
              + newSeat.getSchedule().getTrain().getTrainCode() + " đã có người đặt.");
        }

        // Đánh dấu ghế này đã được chọn trong request này để tránh chọn trùng
        soldSeatIds.add(newSeat.getSeat().getId());


        totalNewPrice += newSeat.getPriceSeat().doubleValue();

        // Merge để đảm bảo Optimistic Locking hoạt động
        scheduleDetailRepository.updateScheduleDetail(em, newSeat);

        // Build vé mới
        Ticket newTicket = Ticket.builder()
            .customer(oldTicket.getCustomer())
            .scheduleDetail(newSeat)
            .type(oldTicket.getType()) // Giữ nguyên đối tượng (Người lớn/Trẻ em)
            .roundTrip(oldTicket.isRoundTrip())
            .originalTicketId(oldTicket.getId()) // Lưu vết vé cũ
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
      invoiceRepository.createInvoice(em, invoice);

      // 8. Tạo chi tiết hóa đơn (InvoiceDetail) gắn với các vé mới
      double subTotalPerTicket = finalAmount / newTickets.size();
      for (Ticket newTicket : newTickets) {
        InvoiceDetail detail = InvoiceDetail.builder()
            .invoice(invoice)
            .ticket(newTicket)
            .subTotal(subTotalPerTicket)
            .isReturned(false)
            .build();
        invoiceDetailRepository.createInvoiceDetail(em, detail);
      }

      // 9. Chốt giao dịch
      transaction.commit();
      log.info("Giao dịch đổi vé hoàn tất. Số lượng: {}", oldTickets.size());
      return Response.success("Đổi vé thành công. Số tiền thanh toán: " + finalAmount, null);

    } catch (OptimisticLockException e) {
      rollbackQuietly(transaction);
      log.warn("Xung đột dữ liệu (Race condition) khi chiếm ghế.");
      return Response.error("Ghế bạn chọn vừa có người khác đặt nhanh hơn. Vui lòng thử lại!");
    } catch (IllegalArgumentException e) {
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

  @Override
  public Response searchTicketsForReturn(ReturnTicketSearchDTO searchDTO) {
    List<String> errors = ValidationUtils.validate(searchDTO);
    if (!errors.isEmpty()) {
      return Response.error("Invalid data: " + String.join(", ", errors));
    }

    EntityManager em = JPAUtils.getEntityManager();
    try {
      List<Ticket> tickets = ticketRepository.findTicketsByCustomerIdCardWithStatus(em, searchDTO.getIdCard(),
          TicketStatus.PAID);
      List<ReturnTicketTicketDTO> result = tickets.stream()
          .map(this::toReturnTicketTicketDTO)
          .toList();
      return Response.success("OK", result);
    } catch (Exception e) {
      log.error("Failed to search tickets for return: idCard={}", searchDTO.getIdCard(), e);
      return Response.error("Failed to search tickets: " + e.getMessage());
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @Override
  public Response previewReturnTickets(ReturnTicketPreviewRequestDTO previewRequestDTO) {
    List<String> errors = ValidationUtils.validate(previewRequestDTO);
    if (!errors.isEmpty()) {
      return Response.error("Invalid data: " + String.join(", ", errors));
    }

    EntityManager em = JPAUtils.getEntityManager();
    try {
      ReturnComputation computation = computeReturn(em, previewRequestDTO.getTicketIds());
      ReturnTicketPreviewDTO preview = ReturnTicketPreviewDTO.builder()
          .totalTicketPrice(computation.totalTicketPrice)
          .refundFee(computation.totalRefundFee)
          .refundAmount(computation.totalRefundAmount)
          .build();
      return Response.success("OK", preview);
    } catch (IllegalArgumentException e) {
      return Response.error(e.getMessage());
    } catch (Exception e) {
      log.error("Failed to preview return tickets", e);
      return Response.error("Failed to preview return tickets: " + e.getMessage());
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @Override
  public Response confirmReturnTickets(ReturnTicketConfirmDTO confirmDTO) {
    List<String> errors = ValidationUtils.validate(confirmDTO);
    if (!errors.isEmpty()) {
      return Response.error("Invalid data: " + String.join(", ", errors));
    }

    EntityManager em = JPAUtils.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();

      ReturnComputation computation = computeReturn(em, confirmDTO.getTicketIds());
      if (Math.abs(confirmDTO.getRefundAmount() - computation.totalRefundAmount) > REFUND_TOLERANCE) {
        return Response.error("Refund amount mismatch. Expected=" + computation.totalRefundAmount);
      }

      Employee employee = employeeRepository.findEmployeeById(em, confirmDTO.getEmployeeId());
      if (employee == null) {
        return Response.error("Employee not found: id=" + confirmDTO.getEmployeeId());
      }

      String customerId = computation.tickets.get(0).getCustomer().getId();
      boolean differentCustomer = computation.tickets.stream()
          .anyMatch(t -> t.getCustomer() == null || !customerId.equals(t.getCustomer().getId()));
      if (differentCustomer) {
        return Response.error("All tickets must belong to the same customer");
      }

      Invoice refundInvoice = Invoice.builder()
          .issueDate(LocalDateTime.now())
          .type(InvoiceType.REFUND)
          .totalAmount(computation.totalRefundAmount)
          .customer(computation.tickets.get(0).getCustomer())
          .employee(employee)
          .build();
      invoiceRepository.createInvoice(em, refundInvoice);

      for (Ticket ticket : computation.tickets) {
        double refundAmount = computation.refundAmountByTicketId.get(ticket.getId());
        double ticketPrice = computation.ticketPriceByTicketId.get(ticket.getId());
        InvoiceDetail refundDetail = InvoiceDetail.builder()
            .invoice(refundInvoice)
            .ticket(ticket)
            .subTotal(ticketPrice)
            .discount(0)
            .insurance(0)
            .isReturned(true)
            .refundAmount(refundAmount)
            .build();
        invoiceDetailRepository.createInvoiceDetail(em, refundDetail);
      }

      List<InvoiceDetail> saleDetails = invoiceDetailRepository.findInvoiceDetailsByTicketIdsAndInvoiceType(em,
          confirmDTO.getTicketIds(), InvoiceType.SALE);
      for (InvoiceDetail detail : saleDetails) {
        detail.setReturned(true);
        String ticketId = detail.getTicket() != null ? detail.getTicket().getId() : null;
        if (ticketId != null && computation.refundAmountByTicketId.containsKey(ticketId)) {
          detail.setRefundAmount(computation.refundAmountByTicketId.get(ticketId));
        }
      }
      invoiceDetailRepository.updateInvoiceDetails(em, saleDetails);

      for (Ticket ticket : computation.tickets) {
        ticket.setStatus(TicketStatus.RETURNED);
        ticket.setQrCode(INVALID_QR_CODE);
      }
      ticketRepository.updateTickets(em, computation.tickets);

      tx.commit();
      return Response.success("Return tickets success", refundInvoice.getId());
    } catch (IllegalArgumentException e) {
      rollbackQuietly(tx);
      return Response.error(e.getMessage());
    } catch (OptimisticLockException e) {
      rollbackQuietly(tx);
      return Response.error("Data conflict - please retry");
    } catch (Exception e) {
      rollbackQuietly(tx);
      log.error("Failed to confirm return tickets", e);
      return Response.error("Failed to return tickets: " + e.getMessage());
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  private ReturnTicketTicketDTO toReturnTicketTicketDTO(Ticket ticket) {
    if (ticket == null) {
      return null;
    }
    String scheduleDetailId = ticket.getScheduleDetail() != null ? ticket.getScheduleDetail().getId() : null;
    String scheduleId = ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSchedule() != null
        ? ticket.getScheduleDetail().getSchedule().getId()
        : null;
    LocalDateTime departureTime = ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSchedule() != null
        ? ticket.getScheduleDetail().getSchedule().getDepartureTime()
        : null;
    double ticketPrice = ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getPriceSeat() != null
        ? ticket.getScheduleDetail().getPriceSeat().doubleValue()
        : 0.0;

    return ReturnTicketTicketDTO.builder()
        .id(ticket.getId())
        .customerId(ticket.getCustomer() != null ? ticket.getCustomer().getId() : null)
        .scheduleDetailId(scheduleDetailId)
        .scheduleId(scheduleId)
        .departureTime(departureTime)
        .ticketPrice(ticketPrice)
        .type(ticket.getType())
        .roundTrip(ticket.isRoundTrip())
        .status(ticket.getStatus())
        .originalTicketId(ticket.getOriginalTicketId())
        .build();
  }

  private ReturnComputation computeReturn(EntityManager em, List<String> ticketIds) {
    if (ticketIds == null || ticketIds.isEmpty()) {
      throw new IllegalArgumentException("ticketIds is required");
    }
    List<String> distinctIds = ticketIds.stream().distinct().toList();
    if (distinctIds.size() != ticketIds.size()) {
      throw new IllegalArgumentException("ticketIds contains duplicates");
    }

    List<Ticket> tickets = ticketRepository.findTicketsByIdsWithSchedule(em, distinctIds);
    if (tickets.size() != distinctIds.size()) {
      throw new IllegalArgumentException("Some tickets not found");
    }

    LocalDateTime now = LocalDateTime.now();
    double totalTicketPrice = 0.0;
    double totalRefundFee = 0.0;

    Map<String, Double> refundAmountByTicketId = new HashMap<>();
    Map<String, Double> ticketPriceByTicketId = new HashMap<>();

    for (Ticket ticket : tickets) {
      if (ticket.getStatus() != TicketStatus.PAID) {
        throw new IllegalArgumentException("Ticket not returnable: id=" + ticket.getId());
      }
      if (ticket.getScheduleDetail() == null || ticket.getScheduleDetail().getSchedule() == null) {
        throw new IllegalArgumentException("Missing schedule for ticket: id=" + ticket.getId());
      }
      LocalDateTime departureTime = ticket.getScheduleDetail().getSchedule().getDepartureTime();
      if (departureTime == null) {
        throw new IllegalArgumentException("Missing departureTime for ticket: id=" + ticket.getId());
      }
      long minutesToDeparture = Duration.between(now, departureTime).toMinutes();
      if (minutesToDeparture < MINUTES_4H) {
        throw new IllegalArgumentException("Ticket not eligible by time rule (>= 4h): id=" + ticket.getId());
      }

      double price = ticket.getScheduleDetail().getPriceSeat() != null ? ticket.getScheduleDetail().getPriceSeat().doubleValue() : 0.0;
      double feeRate = minutesToDeparture < MINUTES_24H ? 0.20 : 0.10;
      double fee = Math.max(price * feeRate, MIN_RETURN_FEE_PER_TICKET);
      if (fee > price) {
        fee = price;
      }
      double refundAmount = price - fee;

      totalTicketPrice += price;
      totalRefundFee += fee;
      refundAmountByTicketId.put(ticket.getId(), refundAmount);
      ticketPriceByTicketId.put(ticket.getId(), price);
    }

    double totalRefundAmount = totalTicketPrice - totalRefundFee;
    if (totalRefundAmount < 0) {
      totalRefundAmount = 0;
    }

    return new ReturnComputation(tickets, totalTicketPrice, totalRefundFee, totalRefundAmount, refundAmountByTicketId,
        ticketPriceByTicketId);
  }

  private record ReturnComputation(
      List<Ticket> tickets,
      double totalTicketPrice,
      double totalRefundFee,
      double totalRefundAmount,
      Map<String, Double> refundAmountByTicketId,
      Map<String, Double> ticketPriceByTicketId) {
  }

  private void rollbackQuietly(EntityTransaction transaction) {
    if (transaction != null && transaction.isActive()) {
      transaction.rollback();
    }
  }
}
