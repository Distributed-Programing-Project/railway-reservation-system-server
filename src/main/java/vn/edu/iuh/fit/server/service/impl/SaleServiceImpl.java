package vn.edu.iuh.fit.server.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import vn.edu.iuh.fit.common.constant.DocumentType;
import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.common.constant.PaymentMethod;
import vn.edu.iuh.fit.common.constant.PaymentStatus;
import vn.edu.iuh.fit.common.constant.SeatAvailabilityStatus;
import vn.edu.iuh.fit.common.constant.StatusSchedule;
import vn.edu.iuh.fit.common.constant.TicketCategory;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.common.constant.TicketType;
import vn.edu.iuh.fit.common.constant.TripDirection;
import vn.edu.iuh.fit.common.dto.CarriageSeatMapDTO;
import vn.edu.iuh.fit.common.dto.IssuedTicketDTO;
import vn.edu.iuh.fit.common.dto.SaleBuyerDTO;
import vn.edu.iuh.fit.common.dto.SaleChildUnder6DTO;
import vn.edu.iuh.fit.common.dto.SaleCreateRequestDTO;
import vn.edu.iuh.fit.common.dto.SaleCreateResponseDTO;
import vn.edu.iuh.fit.common.dto.SalePassengerDTO;
import vn.edu.iuh.fit.common.dto.SaleScheduleSearchDTO;
import vn.edu.iuh.fit.common.dto.SaleScheduleSearchResultDTO;
import vn.edu.iuh.fit.common.dto.ScheduleSaleCardDTO;
import vn.edu.iuh.fit.common.dto.SeatHoldRequestDTO;
import vn.edu.iuh.fit.common.dto.SeatHoldResponseDTO;
import vn.edu.iuh.fit.common.dto.SeatMapRequestDTO;
import vn.edu.iuh.fit.common.dto.SeatMapResponseDTO;
import vn.edu.iuh.fit.common.dto.SeatMapSeatDTO;
import vn.edu.iuh.fit.common.dto.StationDTO;
import vn.edu.iuh.fit.common.message.SaleMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.entity.InvoiceMetadata;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Customer;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Invoice;
import vn.edu.iuh.fit.server.model.InvoiceDetail;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.model.Ticket;
import vn.edu.iuh.fit.server.payment.InternalPaymentOrderStore;
import vn.edu.iuh.fit.server.repository.InvoiceDetailRepository;
import vn.edu.iuh.fit.server.repository.InvoiceRepository;
import vn.edu.iuh.fit.server.repository.ScheduleDetailRepository;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.repository.StationRepository;
import vn.edu.iuh.fit.server.repository.TicketRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.InvoiceDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.InvoiceRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.StationRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.TicketRepositoryImpl;
import vn.edu.iuh.fit.server.service.SaleService;

public class SaleServiceImpl implements SaleService {

  private static final Logger log = LoggerFactory.getLogger(SaleServiceImpl.class);
  private static final int MAX_TICKETS_PER_LEG = 10;
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final double POINT_REDEEM_VALUE = 1_000.0;
  private static final double POINT_EARN_VALUE = 10_000.0;
  private static final double MAX_REDEEM_RATE = 0.10;

  private final StationRepository stationRepository = new StationRepositoryImpl();
  private final ScheduleRepository scheduleRepository = new ScheduleRepositoryImpl();
  private final ScheduleDetailRepository scheduleDetailRepository = new ScheduleDetailRepositoryImpl();
  private final TicketRepository ticketRepository = new TicketRepositoryImpl();
  private final InvoiceRepository invoiceRepository = new InvoiceRepositoryImpl();
  private final InvoiceDetailRepository invoiceDetailRepository = new InvoiceDetailRepositoryImpl();

  @Override
  public Response findAllStations() {
    try {
      return AbstractGenericRepositoryImpl.readOnly(em -> {
        List<Station> stations = stationRepository.findAllStations(em);
        List<StationDTO> dtos = stations.stream()
            .map(s -> StationDTO.builder().id(s.getId()).name(s.getName()).destinationKm(s.getDestinationKm()).build())
            .sorted(Comparator.comparing(StationDTO::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();
        return Response.success(SaleMessages.STATION_LIST_SUCCESS, dtos);
      });
    } catch (Exception e) {
      return Response.error(e.getMessage());
    }
  }

  @Override
  public Response searchSchedulesForSale(SaleScheduleSearchDTO dto) {
    if (dto == null || dto.getDepartureStationId() == null || dto.getDestinationStationId() == null) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }
    if (dto.getDepartureStationId().isBlank() || dto.getDestinationStationId().isBlank()
        || dto.getDepartureStationId().trim().equals(dto.getDestinationStationId().trim())) {
      return Response.error(SaleMessages.INVALID_STATIONS);
    }
    if (dto.getDepartureDate() == null) {
      return Response.error(SaleMessages.INVALID_DATES);
    }
    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP && dto.getReturnDate() == null) {
      return Response.error(SaleMessages.INVALID_DATES);
    }
    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP && dto.getReturnDate().isBefore(dto.getDepartureDate())) {
      return Response.error(SaleMessages.INVALID_DATES);
    }

    try {
      return AbstractGenericRepositoryImpl.readOnly(em -> {
        List<ScheduleSaleCardDTO> outbound = findSaleCardsByDate(em, dto.getDepartureStationId().trim(),
            dto.getDestinationStationId().trim(), dto.getDepartureDate(), dto.getPage(), dto.getSize());

        List<ScheduleSaleCardDTO> returns = List.of();
        if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP) {
          returns = findSaleCardsByDate(em, dto.getDestinationStationId().trim(),
              dto.getDepartureStationId().trim(), dto.getReturnDate(), dto.getPage(), dto.getSize());
        }

        SaleScheduleSearchResultDTO result = SaleScheduleSearchResultDTO.builder()
            .outboundSchedules(outbound)
            .returnSchedules(returns)
            .build();
        return Response.success(SaleMessages.SEARCH_SCHEDULE_SUCCESS, result);
      });
    } catch (Exception e) {
      return Response.error(e.getMessage());
    }
  }

  private List<ScheduleSaleCardDTO> findSaleCardsByDate(jakarta.persistence.EntityManager em,
      String depStationId, String destStationId, LocalDate date, int page, int size) {
    ScheduleRepositoryImpl repo = (ScheduleRepositoryImpl) scheduleRepository;
    List<Schedule> schedules = repo.filterSchedules(em, vn.edu.iuh.fit.common.dto.ScheduleFilterDTO.builder()
        .departureStationId(depStationId)
        .destinationStationId(destStationId)
        .fromDate(date)
        .toDate(date)
        .status(StatusSchedule.NOT_STARTED)
        .page(Math.max(0, page))
        .size(size <= 0 ? 20 : size)
        .build());

    return schedules.stream().map(s -> toSaleCard(em, s)).toList();
  }

  private ScheduleSaleCardDTO toSaleCard(jakarta.persistence.EntityManager em, Schedule schedule) {
    if (schedule == null)
      return null;
    int totalSeats = countTotalSeats(em, schedule.getId());
    int soldSeats = (int) scheduleRepository.countSoldSeatsByScheduleId(em, schedule.getId());
    int availableSeats = Math.max(0, totalSeats - soldSeats);

    Route route = schedule.getRoute();
    Station dep = route != null ? route.getDepartureStation() : null;
    Station dest = route != null ? route.getDestinationStation() : null;
    return ScheduleSaleCardDTO.builder()
        .scheduleId(schedule.getId())
        .trainId(schedule.getTrain() != null ? schedule.getTrain().getId() : null)
        .trainCode(schedule.getTrain() != null ? schedule.getTrain().getTrainCode() : null)
        .routeId(route != null ? route.getId() : null)
        .departureStationName(dep != null ? dep.getName() : null)
        .destinationStationName(dest != null ? dest.getName() : null)
        .departureTime(schedule.getDepartureTime())
        .arrivalTime(schedule.getArrivalTime())
        .availableSeats(availableSeats)
        .totalSeats(totalSeats)
        .status(schedule.getStatus())
        .build();
  }

  private int countTotalSeats(jakarta.persistence.EntityManager em, String scheduleId) {
    Long count = em.createQuery(
        "SELECT COUNT(sd) FROM ScheduleDetail sd WHERE sd.schedule.id = :scheduleId",
        Long.class)
        .setParameter("scheduleId", scheduleId)
        .getSingleResult();
    return count != null ? count.intValue() : 0;
  }

  @Override
  public Response getSeatMapForSchedule(SeatMapRequestDTO dto) {
    if (dto == null || dto.getScheduleId() == null || dto.getScheduleId().isBlank()) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }
    String scheduleId = dto.getScheduleId().trim();
    String sessionId = normalize(dto.getClientSessionId());
    try {
      return AbstractGenericRepositoryImpl.readOnly(em -> {
        Schedule schedule = scheduleRepository.findScheduleById(em, scheduleId);
        if (schedule == null) {
          return Response.error(SaleMessages.NOT_FOUND_SCHEDULE);
        }

        List<ScheduleDetail> details = em.createQuery(
            "SELECT sd FROM ScheduleDetail sd " +
                "JOIN FETCH sd.seat seat " +
                "JOIN FETCH seat.carriage c " +
                "JOIN FETCH sd.schedule sc " +
                "WHERE sc.id = :scheduleId",
            ScheduleDetail.class)
            .setParameter("scheduleId", scheduleId)
            .getResultList();

        Set<String> soldSeatIds = scheduleDetailRepository.getSoldSeatIds(em, scheduleId);
        long now = System.currentTimeMillis();

        Map<String, List<SeatMapSeatDTO>> seatsByCarriageId = new HashMap<>();
        Map<String, Carriage> carriageById = new HashMap<>();
        for (ScheduleDetail sd : details) {
          Seat seat = sd.getSeat();
          if (seat == null)
            continue;
          Carriage carriage = seat.getCarriage();
          if (carriage == null)
            continue;
          carriageById.putIfAbsent(carriage.getId(), carriage);
          double price = sd.getPriceSeat() != null ? sd.getPriceSeat().doubleValue() : 0.0;

          SeatHoldStore.SeatHold hold = SeatHoldStore.getActiveHold(sd.getId(), now);
          boolean sold = soldSeatIds.contains(seat.getId());
          SeatAvailabilityStatus status = sold
              ? SeatAvailabilityStatus.SOLD
              : (hold != null ? SeatAvailabilityStatus.HELD : SeatAvailabilityStatus.AVAILABLE);
          boolean heldByMe = hold != null && sessionId != null && sessionId.equals(hold.clientSessionId());
          Long holdExpiresAt = hold != null ? hold.expiresAtEpochMillis() : null;

          SeatMapSeatDTO seatDTO = SeatMapSeatDTO.builder()
              .seatId(seat.getId())
              .seatNumber(seat.getNumber())
              .seatType(seat.getType())
              .scheduleDetailId(sd.getId())
              .seatPrice(price)
              .seatStatus(status)
              .heldByMe(heldByMe)
              .holdExpiresAtEpochMillis(holdExpiresAt)
              .build();
          seatsByCarriageId.computeIfAbsent(carriage.getId(), k -> new ArrayList<>()).add(seatDTO);
        }

        List<CarriageSeatMapDTO> carriageDTOs = seatsByCarriageId.entrySet().stream()
            .map(entry -> {
              Carriage c = carriageById.get(entry.getKey());
              List<SeatMapSeatDTO> seatDTOs = entry.getValue().stream()
                  .sorted(Comparator.comparingInt(SeatMapSeatDTO::getSeatNumber))
                  .toList();
              return CarriageSeatMapDTO.builder()
                  .carriageId(entry.getKey())
                  .carriageNumber(c != null ? c.getNumber() : 0)
                  .carriageType(c != null ? c.getType() : null)
                  .seats(seatDTOs)
                  .build();
            })
            .sorted(Comparator.comparingInt(CarriageSeatMapDTO::getCarriageNumber))
            .toList();

        SeatMapResponseDTO responseDTO = SeatMapResponseDTO.builder()
            .scheduleId(scheduleId)
            .trainCode(schedule.getTrain() != null ? schedule.getTrain().getTrainCode() : null)
            .carriages(carriageDTOs)
            .build();

        return Response.success(SaleMessages.SEATMAP_SUCCESS, responseDTO);
      });
    } catch (Exception e) {
      return Response.error(e.getMessage());
    }
  }

  @Override
  public Response holdSeatsForSale(SeatHoldRequestDTO dto) {
    if (dto == null)
      return Response.error(SaleMessages.INVALID_REQUEST);
    String scheduleId = normalize(dto.getScheduleId());
    String sessionId = normalize(dto.getClientSessionId());
    List<String> ids = dto.getScheduleDetailIds() != null ? dto.getScheduleDetailIds() : List.of();
    if (scheduleId == null || sessionId == null || ids.isEmpty())
      return Response.error(SaleMessages.INVALID_REQUEST);

    long now = System.currentTimeMillis();
    long expiresAt = now + SeatHoldStore.HOLD_TTL_MILLIS;

    try {
      return AbstractGenericRepositoryImpl.readOnly(em -> {
        List<String> validIds = em.createQuery(
            "SELECT sd.id FROM ScheduleDetail sd WHERE sd.schedule.id = :scheduleId AND sd.id IN :ids",
            String.class)
            .setParameter("scheduleId", scheduleId)
            .setParameter("ids", ids)
            .getResultList();

        if (validIds.size() != ids.size()) {
          return Response.error(SaleMessages.INVALID_REQUEST);
        }

        Set<String> soldSdIds = em.createQuery(
            "SELECT DISTINCT t.scheduleDetail.id FROM Ticket t " +
                "WHERE t.scheduleDetail.id IN :ids AND t.status NOT IN (:cancelled, :exchanged, :returned)",
            String.class)
            .setParameter("ids", ids)
            .setParameter("cancelled", TicketStatus.CANCELLED)
            .setParameter("exchanged", TicketStatus.EXCHANGED)
            .setParameter("returned", TicketStatus.RETURNED)
            .getResultStream()
            .collect(Collectors.toSet());

        List<String> success = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String sdId : ids) {
          if (sdId == null || sdId.isBlank()) {
            failed.add(sdId);
            continue;
          }
          if (soldSdIds.contains(sdId)) {
            failed.add(sdId);
            continue;
          }

          boolean ok = SeatHoldStore.tryHold(sdId, sessionId, expiresAt, now);
          if (ok)
            success.add(sdId);
          else
            failed.add(sdId);
        }

        SeatHoldResponseDTO resDto = SeatHoldResponseDTO.builder()
            .successIds(success)
            .failedIds(failed)
            .expiresAtEpochMillis(expiresAt)
            .build();
        return Response.success(SaleMessages.HOLD_SUCCESS, resDto);
      });
    } catch (Exception e) {
      return Response.error(e.getMessage());
    }
  }

  @Override
  public Response releaseHeldSeatsForSale(SeatHoldRequestDTO dto) {
    if (dto == null)
      return Response.error(SaleMessages.INVALID_REQUEST);
    String sessionId = normalize(dto.getClientSessionId());
    List<String> ids = dto.getScheduleDetailIds() != null ? dto.getScheduleDetailIds() : List.of();
    if (sessionId == null || ids.isEmpty())
      return Response.error(SaleMessages.INVALID_REQUEST);

    long now = System.currentTimeMillis();
    List<String> success = new ArrayList<>();
    List<String> failed = new ArrayList<>();

    for (String sdId : ids) {
      if (sdId == null || sdId.isBlank()) {
        failed.add(sdId);
        continue;
      }

      boolean ok = SeatHoldStore.releaseHold(sdId, sessionId, now);
      if (ok)
        success.add(sdId);
      else
        failed.add(sdId);
    }

    SeatHoldResponseDTO resDto = SeatHoldResponseDTO.builder()
        .successIds(success)
        .failedIds(failed)
        .expiresAtEpochMillis(null)
        .build();
    return Response.success(SaleMessages.RELEASE_HOLD_SUCCESS, resDto);
  }

  @Override
  public Response createSaleTransaction(SaleCreateRequestDTO dto) {
    if (dto == null) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }
    String sessionId = normalize(dto.getClientSessionId());
    if (sessionId == null) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }
    if (dto.getOutboundScheduleId() == null || dto.getOutboundScheduleId().isBlank()) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }
    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP
        && (dto.getReturnScheduleId() == null || dto.getReturnScheduleId().isBlank())) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }

    List<String> outboundSdIds = dto.getOutboundScheduleDetailIds() != null ? dto.getOutboundScheduleDetailIds()
        : List.of();
    List<String> returnSdIds = dto.getReturnScheduleDetailIds() != null ? dto.getReturnScheduleDetailIds() : List.of();

    if (outboundSdIds.size() > MAX_TICKETS_PER_LEG || returnSdIds.size() > MAX_TICKETS_PER_LEG) {
      return Response.error(SaleMessages.TOO_MANY_TICKETS_PER_LEG);
    }
    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP && outboundSdIds.size() != returnSdIds.size()) {
      return Response.error(SaleMessages.SEAT_CONSTRAINT_MISMATCH);
    }
    if (dto.getTicketCategory() == TicketCategory.ONE_WAY && !returnSdIds.isEmpty()) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }
    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP) {
      java.util.Set<String> outSet = new java.util.HashSet<>();
      java.util.Set<String> retSet = new java.util.HashSet<>();
      for (String id : outboundSdIds) {
        if (id != null && !outSet.add(id)) {
          return Response.error(SaleMessages.INVALID_REQUEST);
        }
      }
      for (String id : returnSdIds) {
        if (id != null && !retSet.add(id)) {
          return Response.error(SaleMessages.INVALID_REQUEST);
        }
      }
      for (String id : outSet) {
        if (retSet.contains(id)) {
          return Response.error(SaleMessages.INVALID_REQUEST);
        }
      }
    }

    try {
      Response res = AbstractGenericRepositoryImpl.transactional(em -> doCreateSale(em, dto));
      if (res != null && res.isSuccess()) {
        releaseHeldSeatsForSale(SeatHoldRequestDTO.builder()
            .clientSessionId(sessionId)
            .scheduleDetailIds(combineScheduleDetailIds(outboundSdIds, returnSdIds))
            .build());
      }
      return res;
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();

      if (e.getCause() instanceof jakarta.persistence.OptimisticLockException) {
        return Response.error(SaleMessages.SEAT_ALREADY_SOLD);
      }

      if (cause != null && cause.getCause() != null && cause.getCause().getMessage().contains("Duplicate entry")) {
        return Response.error(
            "Lỗi Dữ liệu: Ghế này đã bị kẹt ràng buộc UNIQUE trong Database (có thể do vé cũ đã trả/hủy)");
      }
      return Response.error(e.getMessage());
    }
  }

  // --- HÀM BỔ TRỢ: TỰ ĐỘNG CẬP NHẬT/TẠO HỒ SƠ KHÁCH HÀNG ---
  private Customer ensureCustomerRecord(EntityManager em, String name, String docNum, DocumentType docType,
      String phone, String email) {
    String normalizedDoc = normalize(docNum);
    if (normalizedDoc == null)
      return null;

    Customer existing = em.createQuery(
        "SELECT c FROM Customer c WHERE c.idCard = :doc OR c.passport = :doc", Customer.class)
        .setParameter("doc", normalizedDoc)
        .getResultStream()
        .findFirst()
        .orElse(null);

    if (existing != null) {
      if (phone != null)
        existing.setPhoneNumber(normalize(phone));
      if (email != null)
        existing.setEmail(normalize(email));
      return em.merge(existing);
    }

    Customer created = Customer.builder()
        .name(normalize(name))
        .idCard(docType == DocumentType.ID_CARD ? normalizedDoc : null)
        .passport(docType == DocumentType.PASSPORT ? normalizedDoc : null)
        .phoneNumber(normalize(phone))
        .email(normalize(email))
        .isActive(true)
        .rewardPoints(0)
        .build();

    em.persist(created);
    log.info("Auto-registered new customer: {} - {}", name, normalizedDoc);
    return created;
  }

  private Response doCreateSale(jakarta.persistence.EntityManager em, SaleCreateRequestDTO dto) {
    PaymentMethod paymentMethod = dto.getPaymentMethod();
    if (paymentMethod == null) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }
    if (paymentMethod == PaymentMethod.CASH && (dto.getAmountPaid() == null || dto.getAmountPaid() < 0)) {
      return Response.error(SaleMessages.PAYMENT_NOT_READY);
    }
    if (paymentMethod == PaymentMethod.ONLINE && normalize(dto.getPaymentOrderId()) == null) {
      return Response.error(SaleMessages.ONLINE_PAYMENT_ORDER_REQUIRED);
    }
    String sessionId = normalize(dto.getClientSessionId());
    if (sessionId == null) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }

    Schedule outboundSchedule = scheduleRepository.findScheduleById(em, dto.getOutboundScheduleId().trim());
    if (outboundSchedule == null) {
      return Response.error(SaleMessages.NOT_FOUND_SCHEDULE);
    }
    Schedule returnSchedule = null;
    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP) {
      returnSchedule = scheduleRepository.findScheduleById(em, dto.getReturnScheduleId().trim());
      if (returnSchedule == null) {
        return Response.error(SaleMessages.NOT_FOUND_SCHEDULE);
      }

      // 🔥 EDGE CASE 2: KHỨ HỒI XUYÊN KHÔNG (THỜI GIAN VỀ PHẢI SAU THỜI GIAN ĐẾN CỦA
      // CHIỀU ĐI)
      if (!returnSchedule.getDepartureTime().isAfter(outboundSchedule.getArrivalTime())) {
        return Response
            .error("Lỗi thời gian: Thời gian khởi hành của chuyến về phải diễn ra SAU thời gian đến của chuyến đi!");
      }
    }

    SaleBuyerDTO buyerDTO = dto.getBuyer();
    if (buyerDTO == null) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }

    // 1. TỰ ĐỘNG ĐĂNG KÝ NGƯỜI MUA VÀO DB
    Customer buyerCustomer = ensureCustomerRecord(em,
        buyerDTO.getBuyerName(),
        buyerDTO.getDocumentNumber(),
        buyerDTO.getDocumentType(),
        buyerDTO.getBuyerPhone(),
        buyerDTO.getBuyerEmail());

    if (buyerCustomer == null) {
      return Response.error(SaleMessages.CUSTOMER_DOCUMENT_REQUIRED);
    }

    // TÌM NHÂN VIÊN ĐANG THỰC HIỆN GIAO DỊCH
    Employee staff = null;
    if (dto.getEmployeeId() != null && !dto.getEmployeeId().isBlank()) {
      staff = em.find(Employee.class, dto.getEmployeeId().trim());
    }

    List<Ticket> createdTickets = new ArrayList<>();
    List<InvoiceDetail> createdDetails = new ArrayList<>();
    List<IssuedTicketDTO> issuedTickets = new ArrayList<>();
    List<IssuedTicketDTO> childVouchers = new ArrayList<>();

    List<SalePassengerDTO> outboundPassengers = dto.getOutboundPassengers() != null ? dto.getOutboundPassengers()
        : List.of();
    List<SalePassengerDTO> returnPassengers = dto.getReturnPassengers() != null ? dto.getReturnPassengers() : List.of();

    if (outboundPassengers.size() != dto.getOutboundScheduleDetailIds().size()) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }
    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP
        && returnPassengers.size() != dto.getReturnScheduleDetailIds().size()) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }

    // 🔥 EDGE CASE 1: CHỐNG TRÙNG LẶP CCCD TRÊN CÙNG MỘT CHUYẾN TÀU (PHÂN THÂN CHI
    // THUẬT)
    Set<String> outboundDocs = new HashSet<>();
    for (SalePassengerDTO p : outboundPassengers) {
      if (p != null && p.getDocumentNumber() != null && !p.getDocumentNumber().isBlank()) {
        if (!outboundDocs.add(normalize(p.getDocumentNumber()))) {
          return Response.error("Hành khách mang giấy tờ số " + p.getDocumentNumber()
              + " không được mua nhiều hơn 1 vé (vị trí ghế) trên chiều đi!");
        }
      }
    }
    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP) {
      Set<String> returnDocs = new HashSet<>();
      for (SalePassengerDTO p : returnPassengers) {
        if (p != null && p.getDocumentNumber() != null && !p.getDocumentNumber().isBlank()) {
          if (!returnDocs.add(normalize(p.getDocumentNumber()))) {
            return Response.error("Hành khách mang giấy tờ số " + p.getDocumentNumber()
                + " không được mua nhiều hơn 1 vé (vị trí ghế) trên chiều về!");
          }
        }
      }
    }

    // 🔥 EDGE CASE: STRICT ATTACHING (SỐ VÉ NL PHẢI >= SỐ VÉ TE)
    long adultCount = outboundPassengers.stream().filter(p -> p == null || p.getTicketType() != TicketType.CHILD)
        .count()
        + returnPassengers.stream().filter(p -> p == null || p.getTicketType() != TicketType.CHILD).count();
    long childCount = outboundPassengers.stream().filter(p -> p != null && p.getTicketType() == TicketType.CHILD)
        .count()
        + returnPassengers.stream().filter(p -> p != null && p.getTicketType() == TicketType.CHILD).count();

    if (childCount > 0 && adultCount < childCount) {
      return Response
          .error("Số lượng vé Người Lớn phải lớn hơn hoặc bằng số lượng vé Trẻ Em trong cùng một giao dịch (đặt chỗ)!");
    }

    // 🔥 EDGE CASE 3: NHÀ TRẺ DI ĐỘNG (GIỚI HẠN TRẺ DƯỚI 6 TUỔI)
    List<SaleChildUnder6DTO> childrenUnder6 = dto.getChildrenUnder6() != null ? dto.getChildrenUnder6() : List.of();
    Map<String, Long> under6CountMap = childrenUnder6.stream()
        .collect(Collectors.groupingBy(
            c -> c.getAccompanyDirection() + "_" + c.getAccompanyPassengerIndex(),
            Collectors.counting()));
    for (Long count : under6CountMap.values()) {
      if (count > 2) {
        return Response.error("Một vé người lớn chỉ được kèm tối đa 2 trẻ em dưới 6 tuổi (miễn vé ngồi chung)!");
      }
    }

    long now = System.currentTimeMillis();
    List<String> allSdIds = combineScheduleDetailIds(dto.getOutboundScheduleDetailIds(),
        dto.getReturnScheduleDetailIds());
    for (String sdId : allSdIds) {
      SeatHoldStore.SeatHold hold = SeatHoldStore.getActiveHold(sdId, now);
      if (hold == null || !sessionId.equals(hold.clientSessionId())) {
        return Response.error(SaleMessages.SEAT_HELD_BY_OTHER);
      }
    }

    double subtotalAfterTypeDiscount = 0.0;

    // 2. TẠO VÉ VÀ TỰ ĐỘNG ĐĂNG KÝ HÀNH KHÁCH
    subtotalAfterTypeDiscount += createSeatTicketsForLeg(em, TripDirection.OUTBOUND, outboundSchedule,
        dto.getOutboundScheduleDetailIds(), outboundPassengers, buyerCustomer,
        dto.getTicketCategory() == TicketCategory.ROUND_TRIP,
        createdTickets, createdDetails, issuedTickets);

    if (dto.getTicketCategory() == TicketCategory.ROUND_TRIP) {
      subtotalAfterTypeDiscount += createSeatTicketsForLeg(em, TripDirection.RETURN, returnSchedule,
          dto.getReturnScheduleDetailIds(), returnPassengers, buyerCustomer, true, createdTickets, createdDetails,
          issuedTickets);
    }

    for (SaleChildUnder6DTO child : childrenUnder6) {
      Response childResult = createChildVoucher(em, child, dto, outboundSchedule, returnSchedule, buyerCustomer,
          createdTickets, createdDetails, childVouchers);
      if (!childResult.isSuccess())
        return childResult;
    }

    // 🔥 EDGE CASE: DISCOUNT PRORATION (PHÂN BỔ ĐIỂM TÍCH LŨY)
    double pointsDiscount = 0.0;
    int redeemedPoints = 0;
    boolean redeemRequested = buyerDTO.isHasAccount() && dto.getRedeemPoints() != null
        && dto.getRedeemPoints().isRedeemRequested();

    if (redeemRequested && buyerCustomer.getRewardPoints() > 0) {
      // Lọc ra các vé KHÔNG thuộc đối tượng giảm giá (Vé Người lớn Normal)
      List<InvoiceDetail> eligibleDetails = createdDetails.stream()
          .filter(d -> d.getTicket().getType() == TicketType.NORMAL && d.getSubTotal() > 0)
          .toList();

      if (eligibleDetails.isEmpty()) {
        return Response
            .error("Không có vé hợp lệ (Người Lớn không ưu đãi) để áp dụng điểm tích lũy trong giao dịch này!");
      }

      // Tính tổng giá trị của các vé hợp lệ
      double totalEligibleAmount = eligibleDetails.stream().mapToDouble(InvoiceDetail::getSubTotal).sum();

      // Tính mức trần được phép giảm (Tối đa 10% của tổng vé hợp lệ)
      double maxDiscountAllowed = totalEligibleAmount * MAX_REDEEM_RATE;
      int maxPointsByRate = (int) Math.floor(maxDiscountAllowed / POINT_REDEEM_VALUE);

      int requested = Math.max(0, dto.getRedeemPoints().getPointsToRedeem());
      int maxByBalance = buyerCustomer.getRewardPoints();
      int allowed = Math.max(0, Math.min(maxByBalance, maxPointsByRate));
      redeemedPoints = requested <= 0 ? allowed : Math.min(requested, allowed);

      pointsDiscount = redeemedPoints * POINT_REDEEM_VALUE;

      // Phân bổ (Prorate) tiền giảm vào từng vé hợp lệ
      if (pointsDiscount > 0) {
        double discountPerTicket = pointsDiscount / eligibleDetails.size();
        for (InvoiceDetail d : eligibleDetails) {
          d.setDiscount(d.getDiscount() + discountPerTicket);
        }
      }
    }

    double totalAmount = Math.max(0, subtotalAfterTypeDiscount - pointsDiscount);

    double amountPaid;
    double changeAmount;
    if (paymentMethod == PaymentMethod.CASH) {
      amountPaid = dto.getAmountPaid() != null ? dto.getAmountPaid() : 0.0;
      if (amountPaid < totalAmount) {
        return Response.error(SaleMessages.PAYMENT_NOT_READY);
      }
      changeAmount = Math.max(0, amountPaid - totalAmount);
    } else if (paymentMethod == PaymentMethod.ONLINE) {
      String orderId = normalize(dto.getPaymentOrderId());
      InternalPaymentOrderStore.PaymentOrder order = InternalPaymentOrderStore.get(orderId);
      if (order == null) {
        return Response.error(SaleMessages.ONLINE_PAYMENT_ORDER_NOT_FOUND);
      }
      if (order.status() != PaymentStatus.SUCCESS) {
        return Response.error(SaleMessages.ONLINE_PAYMENT_NOT_CONFIRMED);
      }
      if (order.consumedInvoiceId() != null) {
        return Response.error(SaleMessages.ONLINE_PAYMENT_ALREADY_USED);
      }
      if (order.clientSessionId() == null || !sessionId.equals(order.clientSessionId())) {
        return Response.error(SaleMessages.ONLINE_PAYMENT_SESSION_MISMATCH);
      }
      long expected = Math.round(totalAmount);
      long paid = Math.round(order.amount());
      if (expected != paid) {
        return Response.error(SaleMessages.ONLINE_PAYMENT_AMOUNT_MISMATCH);
      }
      amountPaid = totalAmount;
      changeAmount = 0.0;
    } else {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }

    Invoice invoice = Invoice.builder()
        .issueDate(LocalDateTime.now())
        .totalAmount(totalAmount)
        .type(InvoiceType.SALE)
        .customer(buyerCustomer)
        .employee(staff)
        .taxCode(dto.getVat() != null ? normalize(dto.getVat().getTaxCode()) : null)
        .companyName(dto.getVat() != null ? normalize(dto.getVat().getCompanyName()) : null)
        .build();
    invoiceRepository.createInvoice(em, invoice);

    InvoiceMetadata meta = InvoiceMetadata.builder()
        .invoice(invoice)
        .vatAddress(dto.getVat() != null ? normalize(dto.getVat().getAddress()) : null)
        .paymentMethod(paymentMethod)
        .paymentOrderId(normalize(dto.getPaymentOrderId()))
        .paymentReferenceCode(resolvePaymentReferenceCode(dto))
        .build();
    em.persist(meta);

    for (InvoiceDetail detail : createdDetails) {
      detail.setInvoice(invoice);
      invoiceDetailRepository.createInvoiceDetail(em, detail);
    }

    // 4. LUÔN LUÔN TÍCH ĐIỂM CHO NGƯỜI MUA BẤT KỂ LÀ AI
    int earnedPoints = 0;
    earnedPoints = (int) Math.floor(totalAmount / POINT_EARN_VALUE);
    int newPoints = Math.max(0, buyerCustomer.getRewardPoints() - redeemedPoints + earnedPoints);
    buyerCustomer.setRewardPoints(newPoints);
    em.merge(buyerCustomer);

    if (paymentMethod == PaymentMethod.ONLINE) {
      String orderId = normalize(dto.getPaymentOrderId());
      InternalPaymentOrderStore.PaymentOrder consumed = InternalPaymentOrderStore.consumeOrder(orderId, invoice.getId(),
          sessionId);
      if (consumed == null || consumed.consumedInvoiceId() == null) {
        throw new IllegalStateException(SaleMessages.ONLINE_PAYMENT_CONSUME_FAILED);
      }
    }

    SaleCreateResponseDTO responseDTO = SaleCreateResponseDTO.builder()
        .invoiceId(invoice.getId())
        .invoiceDate(invoice.getIssueDate())
        .totalAmount(totalAmount)
        .amountPaid(amountPaid)
        .changeAmount(changeAmount)
        .earnedPoints(earnedPoints)
        .redeemedPoints(redeemedPoints)
        .tickets(issuedTickets)
        .childVouchers(childVouchers)
        .build();
    return Response.success(SaleMessages.SALE_SUCCESS, responseDTO);
  }

  private String resolvePaymentReferenceCode(SaleCreateRequestDTO dto) {
    if (dto.getPaymentMethod() != PaymentMethod.ONLINE)
      return null;
    String orderId = normalize(dto.getPaymentOrderId());
    if (orderId == null)
      return null;
    InternalPaymentOrderStore.PaymentOrder order = InternalPaymentOrderStore.get(orderId);
    return order != null ? order.referenceCode() : null;
  }

  private double createSeatTicketsForLeg(jakarta.persistence.EntityManager em,
      TripDirection direction,
      Schedule schedule,
      List<String> scheduleDetailIds,
      List<SalePassengerDTO> passengers,
      Customer buyerCustomer,
      boolean roundTrip,
      List<Ticket> createdTickets,
      List<InvoiceDetail> createdDetails,
      List<IssuedTicketDTO> issuedTickets) {

    if (scheduleDetailIds == null || scheduleDetailIds.isEmpty())
      return 0.0;
    double subtotal = 0.0;

    for (int i = 0; i < scheduleDetailIds.size(); i++) {
      String sdId = scheduleDetailIds.get(i);
      SalePassengerDTO passengerDTO = passengers.get(i);

      ScheduleDetail sd = scheduleDetailRepository.findById(sdId, em);
      if (sd == null || sd.getSchedule() == null || !Objects.equals(sd.getSchedule().getId(), schedule.getId())) {
        throw new IllegalArgumentException(SaleMessages.INVALID_REQUEST);
      }
      em.lock(sd, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

      if (sd.getPriceSeat() == null || sd.getPriceSeat().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(SaleMessages.PRICE_NOT_CONFIGURED);
      }

      long alreadySold = em.createQuery(
          "SELECT COUNT(t) FROM Ticket t WHERE t.scheduleDetail.id = :sdId " +
              "AND t.status NOT IN (:cancelled, :exchanged, :returned)",
          Long.class)
          .setParameter("sdId", sdId)
          .setParameter("cancelled", TicketStatus.CANCELLED)
          .setParameter("exchanged", TicketStatus.EXCHANGED)
          .setParameter("returned", TicketStatus.RETURNED)
          .getSingleResult();
      if (alreadySold > 0) {
        throw new IllegalArgumentException(SaleMessages.SEAT_ALREADY_SOLD);
      }

      // 3. TỰ ĐỘNG ĐĂNG KÝ HÀNH KHÁCH RIÊNG LẺ
      Customer passengerCustomer = ensureCustomerRecord(em,
          passengerDTO.getPassengerName(),
          passengerDTO.getDocumentNumber(),
          passengerDTO.getDocumentType(),
          null, // Hành khách thường chưa cần SĐT ngay
          null);

      // Nếu không tạo được hồ sơ riêng, gán tạm vào người mua
      if (passengerCustomer == null)
        passengerCustomer = buyerCustomer;

      double base = sd.getPriceSeat().doubleValue();
      Pricing pricing = applyPassengerPricing(passengerDTO, schedule.getDepartureTime(), base,
          direction == TripDirection.RETURN);

      Ticket ticket = Ticket.builder()
          .customer(passengerCustomer)
          .scheduleDetail(sd)
          .type(pricing.effectiveType())
          .roundTrip(roundTrip)
          .status(TicketStatus.PAID)
          .qrCode(null)
          .originalTicketId(null)
          .exchanged(false)
          .passengerName(normalize(passengerDTO.getPassengerName()))
          .passengerIdCard(normalize(passengerDTO.getDocumentNumber()))
          .build();
      ticketRepository.createTicket(ticket, em);
      ticket.setQrCode(ticket.getId());
      em.merge(ticket);

      InvoiceDetail detail = InvoiceDetail.builder()
          .invoice(null)
          .ticket(ticket)
          .subTotal(base)
          .discount(pricing.discountAmount())
          .insurance(pricing.insurance())
          .isReturned(false)
          .refundAmount(0.0)
          .build();

      createdTickets.add(ticket);
      createdDetails.add(detail);

      issuedTickets.add(toIssuedTicket(schedule, sd, ticket, pricing.finalPrice(), false, null));
      subtotal += pricing.finalPrice();
    }

    return subtotal;
  }

  private Response createChildVoucher(jakarta.persistence.EntityManager em,
      SaleChildUnder6DTO child,
      SaleCreateRequestDTO request,
      Schedule outboundSchedule,
      Schedule returnSchedule,
      Customer buyerCustomer,
      List<Ticket> createdTickets,
      List<InvoiceDetail> createdDetails,
      List<IssuedTicketDTO> childVouchers) {

    if (child == null)
      return Response.error(SaleMessages.INVALID_REQUEST);
    LocalDate dob = child.getDateOfBirth();
    if (dob == null)
      return Response.error(SaleMessages.INVALID_REQUEST);
    TripDirection dir = child.getAccompanyDirection();
    if (dir == null)
      return Response.error(SaleMessages.INVALID_REQUEST);

    Schedule schedule = dir == TripDirection.OUTBOUND ? outboundSchedule : returnSchedule;
    if (schedule == null)
      return Response.error(SaleMessages.INVALID_REQUEST);
    int age = ageAt(dob, schedule.getDepartureTime());
    if (age >= 6) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }

    List<SalePassengerDTO> passengers = dir == TripDirection.OUTBOUND
        ? (request.getOutboundPassengers() != null ? request.getOutboundPassengers() : List.of())
        : (request.getReturnPassengers() != null ? request.getReturnPassengers() : List.of());

    if (child.getAccompanyPassengerIndex() < 0 || child.getAccompanyPassengerIndex() >= passengers.size()) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }

    SalePassengerDTO adult = passengers.get(child.getAccompanyPassengerIndex());
    if (adult == null)
      return Response.error(SaleMessages.INVALID_REQUEST);
    if (adult.getTicketType() == TicketType.CHILD) {
      return Response.error(SaleMessages.INVALID_REQUEST);
    }

    Ticket voucher = Ticket.builder()
        .customer(buyerCustomer)
        .scheduleDetail(null)
        .type(TicketType.CHILD)
        .roundTrip(request.getTicketCategory() == TicketCategory.ROUND_TRIP)
        .status(TicketStatus.PAID)
        .qrCode(null)
        .originalTicketId(null)
        .exchanged(false)
        .passengerName(normalize(child.getChildName()))
        .passengerIdCard(null)
        .build();
    ticketRepository.createTicket(voucher, em);
    voucher.setQrCode(voucher.getId());
    em.merge(voucher);

    InvoiceDetail detail = InvoiceDetail.builder()
        .invoice(null)
        .ticket(voucher)
        .subTotal(0.0)
        .discount(0.0)
        .insurance(0.0)
        .isReturned(false)
        .refundAmount(0.0)
        .build();

    createdTickets.add(voucher);
    createdDetails.add(detail);

    String accompanyTicketId = null;
    if (dir == TripDirection.OUTBOUND && request.getOutboundScheduleDetailIds() != null) {
      accompanyTicketId = findAccompanyTicketId(createdTickets, adult.getPassengerName());
    }

    childVouchers.add(IssuedTicketDTO.builder()
        .ticketId(voucher.getId())
        .passengerName(voucher.getPassengerName())
        .passengerDocument(null)
        .scheduleId(schedule.getId())
        .trainCode(schedule.getTrain() != null ? schedule.getTrain().getTrainCode() : null)
        .departureStation(schedule.getRoute() != null && schedule.getRoute().getDepartureStation() != null
            ? schedule.getRoute().getDepartureStation().getName()
            : null)
        .destinationStation(schedule.getRoute() != null && schedule.getRoute().getDestinationStation() != null
            ? schedule.getRoute().getDestinationStation().getName()
            : null)
        .departureTime(schedule.getDepartureTime())
        .carriageName("—")
        .seatNumber("Không ghế")
        .seatType(null)
        .ticketType(TicketType.CHILD)
        .price(0.0)
        .qrCode(voucher.getQrCode())
        .childUnder6(true)
        .accompanyAdultTicketId(accompanyTicketId)
        .build());

    return Response.success("OK", null);
  }

  private String findAccompanyTicketId(List<Ticket> createdTickets, String passengerName) {
    if (createdTickets == null)
      return null;
    String normalizedName = normalize(passengerName);
    if (normalizedName == null)
      return null;
    return createdTickets.stream()
        .filter(t -> t.getScheduleDetail() != null)
        .filter(t -> normalizedName.equalsIgnoreCase(normalize(t.getPassengerName())))
        .map(Ticket::getId)
        .findFirst()
        .orElse(null);
  }

  private IssuedTicketDTO toIssuedTicket(Schedule schedule, ScheduleDetail sd, Ticket ticket, double finalPrice,
      boolean childUnder6, String accompanyAdultTicketId) {
    Seat seat = sd.getSeat();
    Carriage carriage = seat != null ? seat.getCarriage() : null;
    Route route = schedule.getRoute();
    Station dep = route != null ? route.getDepartureStation() : null;
    Station dest = route != null ? route.getDestinationStation() : null;

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
        .price(finalPrice)
        .qrCode(ticket.getQrCode())
        .childUnder6(childUnder6)
        .accompanyAdultTicketId(accompanyAdultTicketId)
        .build();
  }

  private Pricing applyPassengerPricing(SalePassengerDTO passenger, LocalDateTime departureTime, double base,
      boolean isReturnTicket) {
    double insurance = 2000.0;

    if (passenger == null) {
      double rawPrice = (base + insurance);
      if (isReturnTicket)
        rawPrice = rawPrice * 0.9;
      double finalPrice = Math.ceil(rawPrice / 1000.0) * 1000.0;
      return new Pricing(TicketType.NORMAL, 0.0, insurance, finalPrice);
    }

    TicketType type = passenger.getTicketType() != null ? passenger.getTicketType() : TicketType.NORMAL;

    // 🔥 BACKEND TỰ ĐỘNG KIỂM TRA TUỔI THỰC TẾ
    if (type == TicketType.CHILD || type == TicketType.SENIOR) {
      if (passenger.getDateOfBirth() == null) {
        throw new IllegalArgumentException("Bắt buộc phải cung cấp Ngày sinh để áp dụng loại vé "
            + (type == TicketType.CHILD ? "Trẻ em" : "Người cao tuổi"));
      }

      int realAge = ageAt(passenger.getDateOfBirth(), departureTime);

      if (type == TicketType.CHILD) {
        if (realAge < 6) {
          throw new IllegalArgumentException("Hành khách " + realAge
              + " tuổi (Dưới 6 tuổi) thuộc diện miễn vé ngồi chung, vui lòng khai báo ở mục Trẻ em dưới 6 tuổi.");
        } else if (realAge >= 10) {
          throw new IllegalArgumentException(
              "Hành khách đã " + realAge + " tuổi, không đủ điều kiện mua vé Trẻ em (Từ 6 đến dưới 10 tuổi).");
        }
      }

      if (type == TicketType.SENIOR && realAge < 60) {
        throw new IllegalArgumentException(
            "Hành khách " + realAge + " tuổi, chưa đủ điều kiện mua vé Người cao tuổi (Từ 60 tuổi trở lên).");
      }
    }

    double discountRate = 0.0;

    if (type == TicketType.CHILD) {
      discountRate = 0.25;
    } else if (type == TicketType.SENIOR) {
      discountRate = 0.15;
    } else if (type == TicketType.STUDENT) {
      discountRate = 0.10;
    }

    double priceWithInsurance = base + insurance;
    double priceAfterTargetDiscount = priceWithInsurance * (1.0 - discountRate);

    if (isReturnTicket) {
      priceAfterTargetDiscount = priceAfterTargetDiscount * 0.9;
    }

    double finalPrice = Math.ceil(priceAfterTargetDiscount / 1000.0) * 1000.0;
    double totalDiscountAmount = priceWithInsurance - finalPrice;

    return new Pricing(type, totalDiscountAmount, insurance, finalPrice);
  }

  private int ageAt(LocalDate dob, LocalDateTime departureTime) {
    if (dob == null || departureTime == null)
      return 0;
    return Period.between(dob, departureTime.toLocalDate()).getYears();
  }

  private String normalize(String value) {
    if (value == null)
      return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private record Pricing(TicketType effectiveType, double discountAmount, double insurance, double finalPrice) {
  }

  private static List<String> combineScheduleDetailIds(List<String> outbound, List<String> returns) {
    List<String> out = new ArrayList<>();
    if (outbound != null)
      out.addAll(outbound);
    if (returns != null)
      out.addAll(returns);
    return out;
  }

  public static double getDistanceMultiplier(double km) {
    if (km <= 100)
      return 1.1;
    if (km <= 300)
      return 1.25;
    if (km <= 800)
      return 1.5;
    return 2.0;
  }

  public static double getSeatMultiplier(String seatType) {
    if (seatType == null)
      return 1.0;

    switch (seatType) {
      case "HARD_SEAT":
        return 1.0;
      case "SOFT_SEAT":
        return 1.2;
      case "BERTH_4":
        return 1.4;
      case "BERTH_6":
        return 1.6;
      case "VIP_SEAT":
        return 2.0;
      default:
        return 1.0;
    }
  }
}