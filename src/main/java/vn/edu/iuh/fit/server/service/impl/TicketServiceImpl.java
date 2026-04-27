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

import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.common.dto.ExchangeEligibleTicketDTO;
import vn.edu.iuh.fit.common.dto.ExchangeEligibleTicketSearchDTO;
import vn.edu.iuh.fit.common.dto.ExchangeTicketRequestDTO;
import vn.edu.iuh.fit.common.dto.ExchangeTicketResponseDTO;
import vn.edu.iuh.fit.common.dto.ExchangeTicketPreviewDTO;
import vn.edu.iuh.fit.common.dto.ExchangeTicketPreviewRequestDTO;
import vn.edu.iuh.fit.common.dto.IssuedTicketDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketConfirmDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketPreviewDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketPreviewRequestDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketSearchDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketTicketDTO;
import vn.edu.iuh.fit.common.message.TicketMessages;
import vn.edu.iuh.fit.common.message.EmployeeMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.mapper.TicketMapper;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Invoice;
import vn.edu.iuh.fit.server.model.InvoiceDetail;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.model.Ticket;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.InvoiceDetailRepository;
import vn.edu.iuh.fit.server.repository.InvoiceRepository;
import vn.edu.iuh.fit.server.repository.ScheduleDetailRepository;
import vn.edu.iuh.fit.server.repository.TicketRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.InvoiceDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.InvoiceRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.TicketRepositoryImpl;
import vn.edu.iuh.fit.server.service.TicketService;
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
  public Response searchTicketsForExchange(ExchangeEligibleTicketSearchDTO searchDTO) {
    List<String> errors = ValidationUtils.validate(searchDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }

    String idCard = normalize(searchDTO.getIdCard());
    if (idCard == null) {
      return Response.error(TicketMessages.ID_CARD_REQUIRED);
    }

    try {
      return AbstractGenericRepositoryImpl.readOnly(em -> {
        List<Ticket> tickets = ticketRepository.findTicketsByCustomerIdCardWithStatusForExchange(em, idCard, TicketStatus.PAID);
        LocalDateTime now = LocalDateTime.now();

        List<ExchangeEligibleTicketDTO> dtos = tickets.stream()
            .map(t -> toExchangeEligibleTicketDTO(t, now))
            .toList();
        return Response.success(TicketMessages.EXCHANGE_SEARCH_SUCCESS, dtos);
      }, e -> Response.error(TicketMessages.EXCHANGE_SEARCH_FAILED_PREFIX + e.getMessage()));
    } catch (Exception e) {
      return Response.error(TicketMessages.EXCHANGE_SEARCH_FAILED_PREFIX + e.getMessage());
    }
  }

  @Override
  public Response previewExchangeTickets(ExchangeTicketPreviewRequestDTO previewDTO) {
    List<String> errors = ValidationUtils.validate(previewDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }
    if (previewDTO.getOldTicketIds().size() != previewDTO.getNewScheduleDetailIds().size()) {
      return Response.error(TicketMessages.COUNT_MISMATCH);
    }
    String sessionId = normalize(previewDTO.getClientSessionId());
    if (sessionId == null) {
      return Response.error(TicketMessages.INVALID_SESSION);
    }

    try {
      return AbstractGenericRepositoryImpl.readOnly(em -> {
        List<Ticket> oldTickets = ticketRepository.findTicketsForExchange(previewDTO.getOldTicketIds(), em);
        if (oldTickets.size() != previewDTO.getOldTicketIds().size()) {
          throw new IllegalArgumentException(TicketMessages.SOME_TICKETS_INVALID);
        }
        validateBusinessRulesOrThrow(oldTickets);

        long nowEpoch = System.currentTimeMillis();
        for (String sdId : previewDTO.getNewScheduleDetailIds()) {
          if (!SeatHoldStore.isHeldBy(sdId, sessionId, nowEpoch)) {
            throw new IllegalStateException(TicketMessages.SEAT_HELD_BY_OTHER);
          }
        }

        double totalOldPrice = oldTickets.stream()
            .map(Ticket::getScheduleDetail)
            .map(sd -> sd != null && sd.getPriceSeat() != null ? sd.getPriceSeat().doubleValue() : 0.0)
            .reduce(0.0, Double::sum);

        List<ScheduleDetail> newSeats = scheduleDetailRepository.findByIdsWithSeatAndSchedule(em, previewDTO.getNewScheduleDetailIds());
        if (newSeats.size() != previewDTO.getNewScheduleDetailIds().size()) {
          throw new IllegalArgumentException(TicketMessages.SOME_TICKETS_INVALID);
        }
        double totalNewPrice = newSeats.stream()
            .map(sd -> sd.getPriceSeat() != null ? sd.getPriceSeat().doubleValue() : 0.0)
            .reduce(0.0, Double::sum);

        double feeTotal = oldTickets.size() * EXCHANGE_FEE;
        double diff = totalNewPrice - totalOldPrice;
        double totalAmount = feeTotal + diff;

        ExchangeTicketPreviewDTO preview = ExchangeTicketPreviewDTO.builder()
            .totalOldPrice(totalOldPrice)
            .totalNewPrice(totalNewPrice)
            .exchangeFeePerTicket(EXCHANGE_FEE)
            .exchangeFeeTotal(feeTotal)
            .priceDifference(diff)
            .totalAmount(totalAmount)
            .build();
        return Response.success(TicketMessages.EXCHANGE_PREVIEW_SUCCESS, preview);
      }, e -> Response.error(TicketMessages.EXCHANGE_PREVIEW_FAILED_PREFIX + e.getMessage()));
    } catch (IllegalArgumentException | IllegalStateException e) {
      return Response.error(e.getMessage());
    } catch (RuntimeException e) {
      return Response.error(TicketMessages.EXCHANGE_PREVIEW_FAILED_PREFIX + e.getMessage());
    }
  }

  @Override
  public Response exchangeTickets(ExchangeTicketRequestDTO requestDTO) {
    List<String> errors = ValidationUtils.validate(requestDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }
    if (requestDTO.getOldTicketIds().stream().anyMatch(id -> id == null || id.isBlank())) {
      return Response.error(TicketMessages.OLD_TICKET_IDS_REQUIRED);
    }
    if (requestDTO.getNewScheduleDetailIds().stream().anyMatch(id -> id == null || id.isBlank())) {
      return Response.error(TicketMessages.NEW_SCHEDULE_DETAIL_IDS_REQUIRED);
    }
    if (requestDTO.getOldTicketIds().size() != requestDTO.getNewScheduleDetailIds().size()) {
      return Response.error(TicketMessages.COUNT_MISMATCH);
    }

    try {
      ExchangeTicketResponseDTO responseData = AbstractGenericRepositoryImpl.transactional(em -> doExchangeTicketsOrThrow(requestDTO, em));
      SeatHoldStore.releaseAll(requestDTO.getNewScheduleDetailIds(), normalize(requestDTO.getClientSessionId()));
      return Response.success(String.format(TicketMessages.EXCHANGE_SUCCESS, responseData.getTotalAmount()), responseData);
    } catch (IllegalArgumentException e) {
      return Response.error(e.getMessage());
    } catch (RuntimeException e) {
      if (e.getCause() instanceof jakarta.persistence.OptimisticLockException) {
        log.warn("Xung đột dữ liệu khi chiếm ghế.");
        return Response.error(TicketMessages.DATA_CONFLICT);
      }
      log.error("Lỗi nghiệp vụ đổi vé: ", e);
      return Response.error(TicketMessages.EXCHANGE_FAILED_PREFIX + e.getMessage());
    }
  }

  private ExchangeTicketResponseDTO doExchangeTicketsOrThrow(ExchangeTicketRequestDTO requestDTO, jakarta.persistence.EntityManager em) {
    Employee employee = employeeRepository.findEmployeeById(em, requestDTO.getEmployeeId());
    if (employee == null) {
      throw new IllegalArgumentException(EmployeeMessages.notFoundById(requestDTO.getEmployeeId()));
    }

    List<Ticket> oldTickets = ticketRepository.findTicketsForExchange(requestDTO.getOldTicketIds(), em);
    if (oldTickets.size() != requestDTO.getOldTicketIds().size()) {
      throw new IllegalArgumentException(TicketMessages.SOME_TICKETS_INVALID);
    }

    validateBusinessRulesOrThrow(oldTickets);

    String sessionId = normalize(requestDTO.getClientSessionId());
    if (sessionId == null) {
      throw new IllegalArgumentException(TicketMessages.INVALID_SESSION);
    }
    long nowEpoch = System.currentTimeMillis();
    for (String sdId : requestDTO.getNewScheduleDetailIds()) {
      if (!SeatHoldStore.isHeldBy(sdId, sessionId, nowEpoch)) {
        throw new IllegalStateException(TicketMessages.SEAT_HELD_BY_OTHER);
      }
    }

    Map<String, Ticket> ticketMap = oldTickets.stream()
        .collect(Collectors.toMap(Ticket::getId, t -> t));

    double totalOldPrice = 0;
    for (Ticket oldTicket : oldTickets) {
      totalOldPrice += oldTicket.getScheduleDetail().getPriceSeat().doubleValue();
      oldTicket.setExchanged(true);
      oldTicket.setStatus(TicketStatus.EXCHANGED);
      oldTicket.setQrCode(INVALID_QR_CODE);
    }
    ticketRepository.updateTickets(em, oldTickets);

    List<String> oldSdIds = oldTickets.stream()
        .map(Ticket::getScheduleDetail)
        .filter(sd -> sd != null)
        .map(ScheduleDetail::getId)
        .distinct()
        .toList();
    if (!oldSdIds.isEmpty()) {
      List<ScheduleDetail> sds = scheduleDetailRepository.findByIdsWithSeatAndSchedule(em, oldSdIds);
      for (ScheduleDetail sd : sds) {
        if (sd.getSeat() != null) {
          sd.getSeat().setAvailable(true);
        }
      }
    }

    List<String> newSeatIds = requestDTO.getNewScheduleDetailIds();
    Map<String, ScheduleDetail> newSeatMap = scheduleDetailRepository
        .findByIdsWithSeatAndSchedule(em, newSeatIds)
        .stream()
        .collect(Collectors.toMap(ScheduleDetail::getId, sd -> sd));

    double totalNewPrice = 0;
    List<Ticket> newTickets = new ArrayList<>();
    List<IssuedTicketDTO> issued = new ArrayList<>();
    Map<String, Set<String>> soldSeatIdsMap = new HashMap<>();

    for (int i = 0; i < newSeatIds.size(); i++) {
      String oldTicketId = requestDTO.getOldTicketIds().get(i);
      String newSeatId = newSeatIds.get(i);
      Ticket oldTicket = ticketMap.get(oldTicketId);
      ScheduleDetail newSeat = newSeatMap.get(newSeatId);

      if (newSeat == null) {
        throw new IllegalArgumentException(TicketMessages.scheduleDetailNotFound(newSeatId));
      }

      LocalDateTime newDepartureTime = newSeat.getSchedule().getDepartureTime();
      if (newDepartureTime == null || newDepartureTime.isBefore(LocalDateTime.now())) {
        throw new IllegalArgumentException("Ghế mới có thời gian khởi hành không hợp lệ hoặc đã qua.");
      }

      String scheduleId = newSeat.getSchedule().getId();
      Set<String> soldSeatIds = soldSeatIdsMap.computeIfAbsent(scheduleId,
          id -> scheduleDetailRepository.getSoldSeatIdsWithLock(em, id));

      if (soldSeatIds.contains(newSeat.getSeat().getId())) {
        throw new IllegalArgumentException(String.format(TicketMessages.SEAT_NOT_AVAILABLE,
            newSeat.getSeat().getNumber(), newSeat.getSchedule().getTrain().getTrainCode()));
      }

      soldSeatIds.add(newSeat.getSeat().getId());
      totalNewPrice += newSeat.getPriceSeat().doubleValue();

      newSeat.getSeat().setAvailable(false);
      Ticket newTicket = Ticket.builder()
          .customer(oldTicket.getCustomer())
          .scheduleDetail(newSeat)
          .type(oldTicket.getType())
          .roundTrip(oldTicket.isRoundTrip())
          .originalTicketId(oldTicket.getId())
          .status(TicketStatus.PAID)
          .exchanged(false)
          .passengerName(oldTicket.getPassengerName())
          .passengerIdCard(oldTicket.getPassengerIdCard())
          .build();

      ticketRepository.createTicket(newTicket, em);
      newTicket.setQrCode(newTicket.getId());
      em.merge(newTicket);
      newTickets.add(newTicket);
      issued.add(toIssuedTicket(newTicket));
    }

    em.flush();
    for (Ticket newTicket : newTickets) {
      em.merge(newTicket.getScheduleDetail().getSeat());
    }

    double totalFee = oldTickets.size() * EXCHANGE_FEE;
    double finalAmount = totalFee + (totalNewPrice - totalOldPrice);

    Invoice invoice = Invoice.builder()
        .issueDate(LocalDateTime.now())
        .type(InvoiceType.EXCHANGE)
        .totalAmount(finalAmount)
        .customer(oldTickets.get(0).getCustomer())
        .employee(employee)
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

    log.info("Giao dịch đổi vé hoàn tất. Số lượng: {}", oldTickets.size());
    return ExchangeTicketResponseDTO.builder()
        .invoiceId(invoice.getId())
        .totalAmount(finalAmount)
        .oldTicketCount(oldTickets.size())
        .newTicketCount(newTickets.size())
        .newTickets(issued)
        .build();
  }

  private void validateBusinessRulesOrThrow(List<Ticket> oldTickets) {
    LocalDateTime now = LocalDateTime.now();
    for (Ticket t : oldTickets) {
      if (t.isExchanged() || t.getOriginalTicketId() != null) {
        throw new IllegalArgumentException(String.format(TicketMessages.TICKET_ALREADY_EXCHANGED, t.getId()));
      }
      if (t.getStatus() == TicketStatus.RETURNED) {
        throw new IllegalArgumentException(String.format(TicketMessages.TICKET_ALREADY_RETURNED, t.getId()));
      }
      if (t.getStatus() != TicketStatus.PAID) {
        throw new IllegalArgumentException(String.format(TicketMessages.TICKET_NOT_PAID, t.getId()));
      }
      LocalDateTime departureTime = t.getScheduleDetail().getSchedule().getDepartureTime();
      if (departureTime != null) {
        long hoursRemaining = ChronoUnit.HOURS.between(now, departureTime);
        if (hoursRemaining < 24) {
          throw new IllegalArgumentException(String.format(TicketMessages.EXCHANGE_TIME_EXPIRED, t.getId(), hoursRemaining));
        }
      }
    }
  }

  private ExchangeEligibleTicketDTO toExchangeEligibleTicketDTO(Ticket ticket, LocalDateTime now) {
    if (ticket == null) return null;
    ScheduleDetail sd = ticket.getScheduleDetail();
    Seat seat = sd != null ? sd.getSeat() : null;
    Carriage carriage = seat != null ? seat.getCarriage() : null;
    vn.edu.iuh.fit.server.model.Schedule schedule = sd != null ? sd.getSchedule() : null;
    Route route = schedule != null ? schedule.getRoute() : null;
    Station dep = route != null ? route.getDepartureStation() : null;
    Station dest = route != null ? route.getDestinationStation() : null;

    Eligibility eligibility = evaluateEligibility(ticket, now);

    return ExchangeEligibleTicketDTO.builder()
        .ticketId(ticket.getId())
        .status(ticket.getStatus())
        .scheduleId(schedule != null ? schedule.getId() : null)
        .departureTime(schedule != null ? schedule.getDepartureTime() : null)
        .trainCode(schedule != null && schedule.getTrain() != null ? schedule.getTrain().getTrainCode() : null)
        .departureStation(dep != null ? dep.getName() : null)
        .destinationStation(dest != null ? dest.getName() : null)
        .scheduleDetailId(sd != null ? sd.getId() : null)
        .seatId(seat != null ? seat.getId() : null)
        .seatType(seat != null ? seat.getType() : null)
        .carriageName(carriage != null ? String.valueOf(carriage.getNumber()) : null)
        .seatNumber(seat != null ? String.valueOf(seat.getNumber()) : null)
        .ticketPrice(sd != null && sd.getPriceSeat() != null ? sd.getPriceSeat().doubleValue() : 0.0)
        .ticketType(ticket.getType())
        .passengerName(ticket.getPassengerName())
        .passengerIdCard(ticket.getPassengerIdCard())
        .eligible(eligibility.eligible)
        .ineligibleReason(eligibility.reason)
        .build();
  }

  private Eligibility evaluateEligibility(Ticket ticket, LocalDateTime now) {
    if (ticket == null) return new Eligibility(false, TicketMessages.SOME_TICKETS_INVALID);
    if (ticket.isExchanged() || ticket.getOriginalTicketId() != null) {
      return new Eligibility(false, String.format(TicketMessages.TICKET_ALREADY_EXCHANGED, ticket.getId()));
    }
    if (ticket.getStatus() == TicketStatus.RETURNED) {
      return new Eligibility(false, String.format(TicketMessages.TICKET_ALREADY_RETURNED, ticket.getId()));
    }
    if (ticket.getStatus() != TicketStatus.PAID) {
      return new Eligibility(false, String.format(TicketMessages.TICKET_NOT_PAID, ticket.getId()));
    }
    ScheduleDetail sd = ticket.getScheduleDetail();
    if (sd == null || sd.getSchedule() == null || sd.getSchedule().getDepartureTime() == null) {
      return new Eligibility(false, TicketMessages.SCHEDULE_NOT_FOUND);
    }
    long hoursRemaining = ChronoUnit.HOURS.between(now, sd.getSchedule().getDepartureTime());
    if (hoursRemaining < 24) {
      return new Eligibility(false, String.format(TicketMessages.EXCHANGE_TIME_EXPIRED, ticket.getId(), hoursRemaining));
    }
    return new Eligibility(true, null);
  }

  private IssuedTicketDTO toIssuedTicket(Ticket ticket) {
    if (ticket == null || ticket.getScheduleDetail() == null || ticket.getScheduleDetail().getSchedule() == null) return null;
    ScheduleDetail sd = ticket.getScheduleDetail();
    vn.edu.iuh.fit.server.model.Schedule schedule = sd.getSchedule();
    Seat seat = sd.getSeat();
    Carriage carriage = seat != null ? seat.getCarriage() : null;
    Route route = schedule.getRoute();
    Station dep = route != null ? route.getDepartureStation() : null;
    Station dest = route != null ? route.getDestinationStation() : null;
    double price = sd.getPriceSeat() != null ? sd.getPriceSeat().doubleValue() : 0.0;

    return IssuedTicketDTO.builder()
        .ticketId(ticket.getId())
        .passengerName(ticket.getPassengerName())
        .passengerDocument(ticket.getPassengerIdCard())
        .scheduleId(schedule.getId())
        .trainCode(schedule.getTrain() != null ? schedule.getTrain().getTrainCode() : null)
        .departureStation(dep != null ? dep.getName() : null)
        .destinationStation(dest != null ? dest.getName() : null)
        .departureTime(schedule.getDepartureTime())
        .carriageName(carriage != null ? String.valueOf(carriage.getNumber()) : null)
        .seatNumber(seat != null ? String.valueOf(seat.getNumber()) : null)
        .seatType(seat != null ? seat.getType() : null)
        .ticketType(ticket.getType())
        .price(price)
        .qrCode(ticket.getQrCode())
        .childUnder6(false)
        .accompanyAdultTicketId(null)
        .build();
  }

  private record Eligibility(boolean eligible, String reason) {
  }

  private String normalize(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  @Override
  public Response searchTicketsForReturn(ReturnTicketSearchDTO searchDTO) {
    List<String> errors = ValidationUtils.validate(searchDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }

    return AbstractGenericRepositoryImpl.readOnly(em -> {
      List<Ticket> tickets = ticketRepository.findTicketsByCustomerIdCardWithStatus(em, searchDTO.getIdCard(),
          TicketStatus.PAID);
      List<ReturnTicketTicketDTO> result = TicketMapper.INSTANCE.toReturnTicketDtoList(tickets);
      return Response.success(TicketMessages.FIND_SUCCESS, result);
    }, e -> {
      log.error("Failed to search tickets for return: idCard={}", searchDTO.getIdCard(), e);
      return Response.error(TicketMessages.SEARCH_FAILED_PREFIX + e.getMessage());
    });
  }

  @Override
  public Response previewReturnTickets(ReturnTicketPreviewRequestDTO previewRequestDTO) {
    List<String> errors = ValidationUtils.validate(previewRequestDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }

    try {
      return AbstractGenericRepositoryImpl.readOnly(em -> {
        ReturnComputation computation = doComputeReturn(em, previewRequestDTO.getTicketIds());
        if (computation.tickets.isEmpty()) {
          return Response.error(TicketMessages.TICKET_IDS_REQUIRED);
        }
        Response mismatch = validateSameCustomer(computation.tickets);
        if (mismatch != null) return mismatch;
        ReturnTicketPreviewDTO preview = ReturnTicketPreviewDTO.builder()
            .totalTicketPrice(computation.totalTicketPrice)
            .refundFee(computation.totalRefundFee)
            .refundAmount(computation.totalRefundAmount)
            .build();
        return Response.success(TicketMessages.PREVIEW_SUCCESS, preview);
      }, e -> {
        log.error("Failed to preview return tickets", e);
        return Response.error(TicketMessages.PREVIEW_FAILED_PREFIX + e.getMessage());
      });
    } catch (IllegalArgumentException e) {
      return Response.error(e.getMessage());
    }
  }

  @Override
  public Response confirmReturnTickets(ReturnTicketConfirmDTO confirmDTO) {
    List<String> errors = ValidationUtils.validate(confirmDTO);
    if (!errors.isEmpty()) {
      return Response.error(TicketMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
    }

    try {
      return AbstractGenericRepositoryImpl.transactional(em -> doConfirmReturnTickets(confirmDTO, em));
    } catch (IllegalArgumentException e) {
      return Response.error(e.getMessage());
    } catch (RuntimeException e) {
      if (e.getCause() instanceof jakarta.persistence.OptimisticLockException) {
        log.warn("Xung đột dữ liệu khi trả vé.");
        return Response.error(TicketMessages.DATA_CONFLICT);
      }
      log.error("Failed to confirm return tickets", e);
      return Response.error(TicketMessages.RETURN_FAILED_PREFIX + e.getMessage());
    }
  }

  private Response validateSameCustomer(List<Ticket> tickets) {
    if (tickets == null || tickets.isEmpty()) {
      return Response.error(TicketMessages.TICKET_IDS_REQUIRED);
    }
    String customerId = tickets.get(0).getCustomer().getId();
    boolean differentCustomer = tickets.stream()
        .anyMatch(t -> t.getCustomer() == null || !customerId.equals(t.getCustomer().getId()));
    if (differentCustomer) {
      return Response.error(TicketMessages.CUSTOMER_MISMATCH);
    }
    return null;
  }

  private Response doConfirmReturnTickets(ReturnTicketConfirmDTO confirmDTO, jakarta.persistence.EntityManager em) {
    ReturnComputation computation = doComputeReturn(em, confirmDTO.getTicketIds());
    if (Math.abs(confirmDTO.getRefundAmount() - computation.totalRefundAmount) > REFUND_TOLERANCE) {
      return Response.error(TicketMessages.REFUND_AMOUNT_MISMATCH);
    }

    Employee employee = employeeRepository.findEmployeeById(em, confirmDTO.getEmployeeId());
    if (employee == null) {
      return Response.error(EmployeeMessages.notFoundById(confirmDTO.getEmployeeId()));
    }

    if (computation.tickets.isEmpty()) {
      return Response.error(TicketMessages.TICKET_IDS_REQUIRED);
    }
    Response mismatch = validateSameCustomer(computation.tickets);
    if (mismatch != null) return mismatch;

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
          .insurance(0) // Insurance is non-refundable per current business rule
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

    List<String> sdIds = computation.tickets.stream()
        .map(Ticket::getScheduleDetail)
        .filter(sd -> sd != null)
        .map(sd -> sd.getId())
        .distinct()
        .toList();
    if (!sdIds.isEmpty()) {
      List<ScheduleDetail> sds = scheduleDetailRepository.findByIdsWithSeatAndSchedule(em, sdIds);
      for (ScheduleDetail sd : sds) {
        if (sd.getSeat() != null) {
          sd.getSeat().setAvailable(true);
        }
      }
      em.flush();
    }

    log.info("Trả vé thành công. Số lượng: {}", computation.tickets.size());
    return Response.success(TicketMessages.RETURN_SUCCESS, refundInvoice.getId());
  }

  private ReturnComputation doComputeReturn(jakarta.persistence.EntityManager em, List<String> ticketIds) {
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
      if (ticket.isExchanged() || ticket.getOriginalTicketId() != null) {
        throw new IllegalArgumentException(String.format(TicketMessages.TICKET_ALREADY_EXCHANGED, ticket.getId()));
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

      double price = ticket.getScheduleDetail().getPriceSeat() != null
          ? ticket.getScheduleDetail().getPriceSeat().doubleValue()
          : 0.0;
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

    return new ReturnComputation(tickets, totalTicketPrice, totalRefundFee, totalRefundAmount,
        refundAmountByTicketId, ticketPriceByTicketId);
  }

  private record ReturnComputation(
      List<Ticket> tickets,
      double totalTicketPrice,
      double totalRefundFee,
      double totalRefundAmount,
      Map<String, Double> refundAmountByTicketId,
      Map<String, Double> ticketPriceByTicketId) {
  }
}
