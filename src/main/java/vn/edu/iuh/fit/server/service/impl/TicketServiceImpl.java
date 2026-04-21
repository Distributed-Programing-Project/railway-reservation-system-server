package vn.edu.iuh.fit.server.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
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
import vn.edu.iuh.fit.server.messages.TicketMessages;
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

  private final TicketRepository ticketRepository = new TicketRepositoryImpl();
  private final ScheduleDetailRepository scheduleDetailRepository = new ScheduleDetailRepositoryImpl();

  @Override
  public Response exchangeTickets(ExchangeTicketRequestDTO requestDTO) {
    // 1. Validate dữ liệu đầu vào
    List<String> errors = ValidationUtils.validate(requestDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }
    if (requestDTO.getOldTicketIds().size() != requestDTO.getNewScheduleDetailIds().size()) {
      return Response.error(TicketMessages.COUNT_MISMATCH);
    }

    // 2. Quản lý Transaction
    EntityManager em = JPAUtils.getEntityManager();
    EntityTransaction transaction = em.getTransaction();

    try {
      transaction.begin();

      // 3. Lấy danh sách vé cũ
      List<Ticket> oldTickets = ticketRepository.findTicketsForExchange(requestDTO.getOldTicketIds(), em);
      if (oldTickets.size() != requestDTO.getOldTicketIds().size()) {
        return Response.error(TicketMessages.SOME_TICKETS_INVALID);
      }

      // 4. Kiểm tra quy tắc nghiệp vụ
      Response validationRes = validateBusinessRules(oldTickets);
      if (validationRes != null) return validationRes;

      Map<String, Ticket> ticketMap = oldTickets.stream()
          .collect(Collectors.toMap(Ticket::getId, t -> t));

      double totalOldPrice = 0;
      double totalNewPrice = 0;
      List<Ticket> newTickets = new ArrayList<>();
      Map<String, Set<String>> soldSeatIdsMap = new HashMap<>();

      // 5. Cập nhật vé cũ
      for (Ticket oldTicket : oldTickets) {
        totalOldPrice += oldTicket.getScheduleDetail().getPriceSeat().doubleValue();
        oldTicket.setExchanged(true);
        oldTicket.setStatus(TicketStatus.EXCHANGED);
        em.merge(oldTicket);
      }

      // 6. Xử lý đặt ghế mới và tạo vé mới
      for (int i = 0; i < requestDTO.getNewScheduleDetailIds().size(); i++) {
        String oldTicketId = requestDTO.getOldTicketIds().get(i);
        String newSeatId = requestDTO.getNewScheduleDetailIds().get(i);
        Ticket oldTicket = ticketMap.get(oldTicketId);

        ScheduleDetail newSeat = scheduleDetailRepository.findById(newSeatId, em);
        if (newSeat == null) {
            throw new IllegalArgumentException("Không tìm thấy lịch trình ghế: " + newSeatId);
        }

        String scheduleId = newSeat.getSchedule().getId();
        Set<String> soldSeatIds = soldSeatIdsMap.computeIfAbsent(scheduleId,
            id -> scheduleDetailRepository.getSoldSeatIds(em, id));

        if (soldSeatIds.contains(newSeat.getSeat().getId())) {
          return Response.error(String.format(TicketMessages.SEAT_NOT_AVAILABLE, 
              newSeat.getSeat().getNumber(), newSeat.getSchedule().getTrain().getTrainCode()));
        }

        soldSeatIds.add(newSeat.getSeat().getId());
        totalNewPrice += newSeat.getPriceSeat().doubleValue();
        em.merge(newSeat);

        Ticket newTicket = Ticket.builder()
            .customer(oldTicket.getCustomer())
            .scheduleDetail(newSeat)
            .type(oldTicket.getType())
            .roundTrip(oldTicket.isRoundTrip())
            .originalTicketId(oldTicket.getId())
            .status(TicketStatus.PAID)
            .exchanged(false)
            .build();

        ticketRepository.createTicket(newTicket, em);
        newTickets.add(newTicket);
      }

      // 7. Tạo Hóa đơn đổi vé
      double totalFee = oldTickets.size() * EXCHANGE_FEE;
      double finalAmount = totalFee + (totalNewPrice - totalOldPrice);

      Invoice invoice = Invoice.builder()
          .issueDate(LocalDateTime.now())
          .type(InvoiceType.EXCHANGE)
          .totalAmount(finalAmount)
          .customer(oldTickets.get(0).getCustomer())
          .taxCode(requestDTO.getTaxCode())
          .companyName(requestDTO.getCompanyName())
          .build();
      em.persist(invoice);

      // 8. Tạo chi tiết hóa đơn
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

      transaction.commit();
      log.info("Giao dịch đổi vé hoàn tất. Số lượng: {}", oldTickets.size());
      return Response.success(String.format(TicketMessages.EXCHANGE_SUCCESS, finalAmount), null);

    } catch (OptimisticLockException e) {
      rollbackQuietly(transaction);
      log.warn("Xung đột dữ liệu khi chiếm ghế.");
      return Response.error("Ghế bạn chọn vừa có người khác đặt nhanh hơn. Vui lòng thử lại!");
    } catch (Exception e) {
      rollbackQuietly(transaction);
      log.error("Lỗi nghiệp vụ đổi vé: ", e);
      return Response.error(TicketMessages.EXCHANGE_FAILED_PREFIX + e.getMessage());
    } finally {
      if (em.isOpen()) em.close();
    }
  }

  private Response validateBusinessRules(List<Ticket> oldTickets) {
    LocalDateTime now = LocalDateTime.now();
    for (Ticket t : oldTickets) {
      if (t.isExchanged() || t.getOriginalTicketId() != null) {
        return Response.error(String.format(TicketMessages.TICKET_ALREADY_EXCHANGED, t.getId()));
      }
      if (t.getStatus() != TicketStatus.PAID) {
        return Response.error(String.format(TicketMessages.TICKET_NOT_PAID, t.getId()));
      }
      LocalDateTime departureTime = t.getScheduleDetail().getSchedule().getDepartureTime();
      if (departureTime != null) {
        long hoursRemaining = ChronoUnit.HOURS.between(now, departureTime);
        if (hoursRemaining < 24) {
          return Response.error(String.format(TicketMessages.EXCHANGE_TIME_EXPIRED, t.getId(), hoursRemaining));
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