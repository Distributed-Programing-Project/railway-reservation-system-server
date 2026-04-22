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
import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.common.dto.ExchangeTicketRequestDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketConfirmDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketPreviewDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketPreviewRequestDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketSearchDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketTicketDTO;
import vn.edu.iuh.fit.common.message.TicketMessages;
import vn.edu.iuh.fit.common.message.EmployeeMessages;
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

  private final TicketRepository ticketRepository = new TicketRepositoryImpl();
  private final ScheduleDetailRepository scheduleDetailRepository = new ScheduleDetailRepositoryImpl();
  private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();
  private final InvoiceRepository invoiceRepository = new InvoiceRepositoryImpl();
  private final InvoiceDetailRepository invoiceDetailRepository = new InvoiceDetailRepositoryImpl();

  @Override
  public Response exchangeTickets(ExchangeTicketRequestDTO requestDTO) {
    List<String> errors = ValidationUtils.validate(requestDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }
    if (requestDTO.getOldTicketIds().size() != requestDTO.getNewScheduleDetailIds().size()) {
      return Response.error(TicketMessages.COUNT_MISMATCH);
    }

    EntityManager em = JPAUtils.getEntityManager();
    EntityTransaction transaction = em.getTransaction();

    try {
      transaction.begin();

      List<Ticket> oldTickets = ticketRepository.findTicketsForExchange(requestDTO.getOldTicketIds(), em);
      if (oldTickets.size() != requestDTO.getOldTicketIds().size()) {
        return Response.error(TicketMessages.SOME_TICKETS_INVALID);
      }

      Response validationRes = validateBusinessRules(oldTickets);
      if (validationRes != null) return validationRes;

      Map<String, Ticket> ticketMap = oldTickets.stream()
          .collect(Collectors.toMap(Ticket::getId, t -> t));

      double totalOldPrice = 0;
      double totalNewPrice = 0;
      List<Ticket> newTickets = new ArrayList<>();
      Map<String, Set<String>> soldSeatIdsMap = new HashMap<>();

      for (Ticket oldTicket : oldTickets) {
        totalOldPrice += oldTicket.getScheduleDetail().getPriceSeat().doubleValue();
        oldTicket.setExchanged(true);
        oldTicket.setStatus(TicketStatus.EXCHANGED);
        ticketRepository.updateTicket(em, oldTicket);
      }

      for (int i = 0; i < requestDTO.getNewScheduleDetailIds().size(); i++) {
        String oldTicketId = requestDTO.getOldTicketIds().get(i);
        String newSeatId = requestDTO.getNewScheduleDetailIds().get(i);
        Ticket oldTicket = ticketMap.get(oldTicketId);

        ScheduleDetail newSeat = scheduleDetailRepository.findById(newSeatId, em);
        if (newSeat == null) {
            throw new IllegalArgumentException(TicketMessages.scheduleDetailNotFound(newSeatId));
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
        scheduleDetailRepository.updateScheduleDetail(em, newSeat);

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
      invoiceRepository.createInvoice(em, invoice);

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

      transaction.commit();
      log.info("Giao dịch đổi vé hoàn tất. Số lượng: {}", oldTickets.size());
      return Response.success(String.format(TicketMessages.EXCHANGE_SUCCESS, finalAmount), null);

    } catch (OptimisticLockException e) {
      rollbackQuietly(transaction);
      log.warn("Xung đột dữ liệu khi chiếm ghế.");
      return Response.error(TicketMessages.DATA_CONFLICT);
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

  @Override
  public Response searchTicketsForReturn(ReturnTicketSearchDTO searchDTO) {
    List<String> errors = ValidationUtils.validate(searchDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }

    EntityManager em = JPAUtils.getEntityManager();
    try {
      List<Ticket> tickets = ticketRepository.findTicketsByCustomerIdCardWithStatus(em, searchDTO.getIdCard(),
          TicketStatus.PAID);
      List<ReturnTicketTicketDTO> result = tickets.stream()
          .map(this::toReturnTicketTicketDTO)
          .toList();
      return Response.success(TicketMessages.FIND_SUCCESS, result);
    } catch (Exception e) {
      log.error("Failed to search tickets for return: idCard={}", searchDTO.getIdCard(), e);
      return Response.error(TicketMessages.SEARCH_FAILED_PREFIX + e.getMessage());
    } finally {
      if (em.isOpen()) em.close();
    }
  }

  @Override
  public Response previewReturnTickets(ReturnTicketPreviewRequestDTO previewRequestDTO) {
    List<String> errors = ValidationUtils.validate(previewRequestDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }

    EntityManager em = JPAUtils.getEntityManager();
    try {
      ReturnComputation computation = computeReturn(em, previewRequestDTO.getTicketIds());
      ReturnTicketPreviewDTO preview = ReturnTicketPreviewDTO.builder()
          .totalTicketPrice(computation.totalTicketPrice)
          .refundFee(computation.totalRefundFee)
          .refundAmount(computation.totalRefundAmount)
          .build();
      return Response.success(TicketMessages.PREVIEW_SUCCESS, preview);
    } catch (IllegalArgumentException e) {
      return Response.error(e.getMessage());
    } catch (Exception e) {
      log.error("Failed to preview return tickets", e);
      return Response.error(TicketMessages.PREVIEW_FAILED_PREFIX + e.getMessage());
    } finally {
      if (em.isOpen()) em.close();
    }
  }

  @Override
  public Response confirmReturnTickets(ReturnTicketConfirmDTO confirmDTO) {
    List<String> errors = ValidationUtils.validate(confirmDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }

    EntityManager em = JPAUtils.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();

      ReturnComputation computation = computeReturn(em, confirmDTO.getTicketIds());
      if (Math.abs(confirmDTO.getRefundAmount() - computation.totalRefundAmount) > REFUND_TOLERANCE) {
        return Response.error(TicketMessages.REFUND_AMOUNT_MISMATCH);
      }

      Employee employee = employeeRepository.findEmployeeById(em, confirmDTO.getEmployeeId());
      if (employee == null) {
        return Response.error(EmployeeMessages.notFoundById(confirmDTO.getEmployeeId()));
      }

      String customerId = computation.tickets.get(0).getCustomer().getId();
      boolean differentCustomer = computation.tickets.stream()
          .anyMatch(t -> t.getCustomer() == null || !customerId.equals(t.getCustomer().getId()));
      if (differentCustomer) {
        return Response.error(TicketMessages.CUSTOMER_MISMATCH);
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
      return Response.success(TicketMessages.RETURN_SUCCESS, refundInvoice.getId());
    } catch (IllegalArgumentException e) {
      rollbackQuietly(tx);
      return Response.error(e.getMessage());
    } catch (OptimisticLockException e) {
      rollbackQuietly(tx);
      return Response.error(TicketMessages.DATA_CONFLICT);
    } catch (Exception e) {
      rollbackQuietly(tx);
      log.error("Failed to confirm return tickets", e);
      return Response.error(TicketMessages.RETURN_FAILED_PREFIX + e.getMessage());
    } finally {
      if (em.isOpen()) em.close();
    }
  }

  private ReturnTicketTicketDTO toReturnTicketTicketDTO(Ticket ticket) {
    if (ticket == null) return null;
    
    return ReturnTicketTicketDTO.builder()
        .id(ticket.getId())
        .customerId(ticket.getCustomer() != null ? ticket.getCustomer().getId() : null)
        .scheduleDetailId(ticket.getScheduleDetail() != null ? ticket.getScheduleDetail().getId() : null)
        .scheduleId(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSchedule() != null
            ? ticket.getScheduleDetail().getSchedule().getId() : null)
        .departureTime(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSchedule() != null
            ? ticket.getScheduleDetail().getSchedule().getDepartureTime() : null)
        .ticketPrice(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getPriceSeat() != null
            ? ticket.getScheduleDetail().getPriceSeat().doubleValue() : 0.0)
        .type(ticket.getType())
        .roundTrip(ticket.isRoundTrip())
        .status(ticket.getStatus())
        .originalTicketId(ticket.getOriginalTicketId())
        .build();
  }

  private ReturnComputation computeReturn(EntityManager em, List<String> ticketIds) {
    if (ticketIds == null || ticketIds.isEmpty()) {
      throw new IllegalArgumentException(TicketMessages.TICKET_IDS_REQUIRED);
    }
    List<String> distinctIds = ticketIds.stream().distinct().toList();
    if (distinctIds.size() != ticketIds.size()) {
      throw new IllegalArgumentException(TicketMessages.TICKET_IDS_DUPLICATE);
    }

    List<Ticket> tickets = ticketRepository.findTicketsByIdsWithSchedule(em, distinctIds);
    if (tickets.size() != distinctIds.size()) {
      throw new IllegalArgumentException(TicketMessages.SOME_TICKETS_INVALID);
    }

    LocalDateTime now = LocalDateTime.now();
    double totalTicketPrice = 0.0;
    double totalRefundFee = 0.0;

    Map<String, Double> refundAmountByTicketId = new HashMap<>();
    Map<String, Double> ticketPriceByTicketId = new HashMap<>();

    for (Ticket ticket : tickets) {
      if (ticket.getStatus() != TicketStatus.PAID) {
        throw new IllegalArgumentException(String.format(TicketMessages.TICKET_NOT_RETURNABLE, ticket.getId()));
      }
      if (ticket.getScheduleDetail() == null || ticket.getScheduleDetail().getSchedule() == null) {
        throw new IllegalArgumentException(TicketMessages.SCHEDULE_NOT_FOUND);
      }
      LocalDateTime departureTime = ticket.getScheduleDetail().getSchedule().getDepartureTime();
      if (departureTime == null) {
        throw new IllegalArgumentException(TicketMessages.SCHEDULE_NOT_FOUND);
      }
      long minutesToDeparture = Duration.between(now, departureTime).toMinutes();
      if (minutesToDeparture < MINUTES_4H) {
        throw new IllegalArgumentException(String.format(TicketMessages.NOT_ELIGIBLE_BY_TIME, ticket.getId()));
      }

      double price = ticket.getScheduleDetail().getPriceSeat() != null ? ticket.getScheduleDetail().getPriceSeat().doubleValue() : 0.0;
      double feeRate = minutesToDeparture < MINUTES_24H ? 0.20 : 0.10;
      double fee = Math.max(price * feeRate, MIN_RETURN_FEE_PER_TICKET);
      if (fee > price) fee = price;
      
      double refundAmount = price - fee;
      totalTicketPrice += price;
      totalRefundFee += fee;
      refundAmountByTicketId.put(ticket.getId(), refundAmount);
      ticketPriceByTicketId.put(ticket.getId(), price);
    }

    double totalRefundAmount = Math.max(0, totalTicketPrice - totalRefundFee);

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
