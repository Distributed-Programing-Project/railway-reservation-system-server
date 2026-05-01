package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.constant.RouteStatus;
import vn.edu.iuh.fit.common.constant.StatusSchedule;
import vn.edu.iuh.fit.common.constant.TrainStatus;
import vn.edu.iuh.fit.common.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleDetailDTO;
import vn.edu.iuh.fit.common.dto.ScheduleDetailPriceUpdateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.common.dto.ScheduleGenerateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleGenerateResultDTO;
import vn.edu.iuh.fit.common.dto.ScheduleLifecycleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleUpdateDTO;
import vn.edu.iuh.fit.common.message.ScheduleMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.mapper.ScheduleMapper;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.ScheduleDetailRepository;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleRepositoryImpl;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScheduleServiceImpl implements ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    private final ScheduleRepository repository = new ScheduleRepositoryImpl();
    private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();
    private final ScheduleDetailRepository scheduleDetailRepository = new ScheduleDetailRepositoryImpl();

    private Response requireActiveManager(String employeeId) {
        return AbstractGenericRepositoryImpl.readOnly(em -> {
            Employee requester = employeeRepository.findEmployeeById(em, employeeId);
            if (requester == null) {
                return Response.error(ScheduleMessages.employeeNotFoundById(employeeId));
            }
            if (requester.getEmployeeStatus() != EmployeeStatus.ACTIVE) {
                return Response.error(ScheduleMessages.EMPLOYEE_INACTIVE);
            }
            if (!Boolean.TRUE.equals(requester.getIsManager())) {
                return Response.error(ScheduleMessages.UNAUTHORIZED);
            }
            return null;
        });
    }

    @Override
    public Response createSchedule(ScheduleCreateDTO scheduleDTO) {
        List<String> errors = ValidationUtils.validate(scheduleDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Response authError = requireActiveManager(scheduleDTO.getRequestEmployeeId());
        if (authError != null) return authError;

        if (scheduleDTO.getDepartureTime().isBefore(LocalDateTime.now().plusDays(1))) {
            return Response.error(ScheduleMessages.DEPARTURE_TIME_MIN_ONE_DAY);
        }
        if (!scheduleDTO.getArrivalTime().isAfter(scheduleDTO.getDepartureTime())) {
            return Response.error(ScheduleMessages.ARRIVAL_TIME_INVALID);
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Train train = em.find(Train.class, scheduleDTO.getTrainId());
                Response trainError = validateActiveTrain(train, scheduleDTO.getTrainId());
                if (trainError != null) return trainError;

                Route route = em.find(Route.class, scheduleDTO.getRouteId());
                Response routeError = validateActiveRoute(route, scheduleDTO.getRouteId());
                if (routeError != null) return routeError;

                Schedule schedule = Schedule.builder()
                        .train(train)
                        .route(route)
                        .departureTime(scheduleDTO.getDepartureTime())
                        .arrivalTime(scheduleDTO.getArrivalTime())
                        .status(StatusSchedule.DRAFT)
                        .build();

                Schedule savedSchedule = repository.createScheduleWithDetails(em, schedule, scheduleDTO.getTrainId());
                log.info("Schedule created: id={}, trainId={}", savedSchedule.getId(), scheduleDTO.getTrainId());
                return Response.success(ScheduleMessages.CREATE_SUCCESS, savedSchedule.getId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to create schedule: trainId={}, routeId={}", scheduleDTO.getTrainId(),
                    scheduleDTO.getRouteId(), e);
            return Response.error(ScheduleMessages.CREATE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response updateSchedule(ScheduleUpdateDTO scheduleUpdateDTO) {
        List<String> errors = ValidationUtils.validate(scheduleUpdateDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Response authError = requireActiveManager(scheduleUpdateDTO.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Schedule existingSchedule = repository.findScheduleById(em, scheduleUpdateDTO.getScheduleId());
                if (existingSchedule == null) {
                    return Response.error(ScheduleMessages.scheduleNotFoundById(scheduleUpdateDTO.getScheduleId()));
                }
                if (existingSchedule.getStatus() != StatusSchedule.DRAFT) {
                    return Response.error(ScheduleMessages.DRAFT_UPDATE_ONLY);
                }

                if (scheduleUpdateDTO.getDepartureTime() != null
                        && !scheduleUpdateDTO.getDepartureTime().isAfter(LocalDateTime.now())) {
                    return Response.error(ScheduleMessages.DEPARTURE_TIME_IN_PAST);
                }
                LocalDateTime departure = scheduleUpdateDTO.getDepartureTime() != null
                        ? scheduleUpdateDTO.getDepartureTime()
                        : existingSchedule.getDepartureTime();
                if (scheduleUpdateDTO.getArrivalTime() != null
                        && !scheduleUpdateDTO.getArrivalTime().isAfter(departure)) {
                    return Response.error(ScheduleMessages.ARRIVAL_TIME_NOT_AFTER_DEPARTURE);
                }

                Train train = em.find(Train.class, scheduleUpdateDTO.getTrainId());
                Response trainError = validateActiveTrain(train, scheduleUpdateDTO.getTrainId());
                if (trainError != null) return trainError;

                Route route = em.find(Route.class, scheduleUpdateDTO.getRouteId());
                Response routeError = validateActiveRoute(route, scheduleUpdateDTO.getRouteId());
                if (routeError != null) return routeError;

                Schedule scheduleToUpdate = ScheduleMapper.INSTANCE.toEntityForUpdate(scheduleUpdateDTO);
                boolean updated = repository.updateSchedule(em, scheduleToUpdate);
                if (!updated) {
                    return Response.error(ScheduleMessages.updateFailedById(scheduleUpdateDTO.getScheduleId()));
                }

                log.info("Schedule updated: id={}", scheduleUpdateDTO.getScheduleId());
                return Response.success(ScheduleMessages.UPDATE_SUCCESS, scheduleUpdateDTO.getScheduleId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to update schedule: id={}", scheduleUpdateDTO.getScheduleId(), e);
            return Response.error(ScheduleMessages.UPDATE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response filterSchedules(ScheduleFilterDTO filter) {
        List<String> errors = ValidationUtils.validate(filter);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Response authError = requireActiveManager(filter.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                List<Schedule> schedules = repository.filterSchedules(em, filter);
                List<ScheduleDTO> scheduleDTOList = ScheduleMapper.INSTANCE.toDtoList(schedules);
                return Response.success(ScheduleMessages.FILTER_SUCCESS, scheduleDTOList);
            });
        } catch (Exception e) {
            log.error("Failed to filter schedules", e);
            return Response.error(ScheduleMessages.FILTER_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response generateSchedules(ScheduleGenerateDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Train train = em.find(Train.class, dto.getTrainId());
                Response trainError = validateActiveTrain(train, dto.getTrainId());
                if (trainError != null) return trainError;

                Route route = em.find(Route.class, dto.getRouteId());
                Response routeError = validateActiveRoute(route, dto.getRouteId());
                if (routeError != null) return routeError;

                Route reverseRoute = null;
                if (dto.isRoundTrip()) {
                    reverseRoute = repository.findActiveReverseRoute(em, dto.getRouteId());
                    if (reverseRoute == null) {
                        return Response.error(ScheduleMessages.REVERSE_ROUTE_NOT_FOUND);
                    }
                }

                List<String> createdIds = new ArrayList<>();
                LocalDateTime minimumDeparture = LocalDateTime.now().plusDays(1);
                for (int day = 0; day < dto.getDays(); day++) {
                    LocalDateTime departure = LocalDateTime.of(dto.getStartDate().plusDays(day), dto.getDepartureTime());
                    if (departure.isBefore(minimumDeparture)) {
                        return Response.error(ScheduleMessages.DEPARTURE_TIME_MIN_ONE_DAY);
                    }
                }

                for (int day = 0; day < dto.getDays(); day++) {
                    LocalDateTime outboundDeparture = LocalDateTime.of(dto.getStartDate().plusDays(day), dto.getDepartureTime());
                    Schedule outbound = buildDraftSchedule(train, route, outboundDeparture, outboundDeparture.plusHours(4));
                    Schedule savedOutbound = repository.createScheduleWithDetails(em, outbound, train.getId());
                    createdIds.add(savedOutbound.getId());

                    if (reverseRoute != null) {
                        LocalDateTime returnDeparture = outboundDeparture.plusHours(4);
                        Schedule inbound = buildDraftSchedule(train, reverseRoute, returnDeparture, returnDeparture.plusHours(4));
                        Schedule savedInbound = repository.createScheduleWithDetails(em, inbound, train.getId());
                        createdIds.add(savedInbound.getId());
                    }
                }

                ScheduleGenerateResultDTO result = ScheduleGenerateResultDTO.builder()
                        .createdCount(createdIds.size())
                        .outboundCount(dto.getDays())
                        .returnCount(dto.isRoundTrip() ? dto.getDays() : 0)
                        .scheduleIds(createdIds)
                        .build();
                log.info("Schedules generated: trainId={}, routeId={}, count={}",
                        dto.getTrainId(), dto.getRouteId(), createdIds.size());
                return Response.success(ScheduleMessages.GENERATE_SUCCESS, result);
            });
        } catch (RuntimeException e) {
            log.error("Failed to generate schedules: trainId={}, routeId={}", dto.getTrainId(), dto.getRouteId(), e);
            return Response.error(ScheduleMessages.GENERATE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response deleteSchedule(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return Response.error(ScheduleMessages.SCHEDULE_ID_REQUIRED);
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Schedule existingSchedule = repository.findScheduleById(em, scheduleId);
                if (existingSchedule == null) {
                    return Response.error(ScheduleMessages.scheduleNotFoundById(scheduleId));
                }
                if (existingSchedule.getStatus() != StatusSchedule.DRAFT) {
                    return Response.error(ScheduleMessages.DRAFT_DELETE_ONLY);
                }

                boolean deleted = repository.deleteSchedule(em, scheduleId);
                if (!deleted) {
                    return Response.error(ScheduleMessages.deleteFailedById(scheduleId));
                }

                log.info("Schedule deleted: id={}", scheduleId);
                return Response.success(ScheduleMessages.DELETE_SUCCESS, scheduleId);
            });
        } catch (RuntimeException e) {
            log.error("Failed to delete schedule: id={}", scheduleId, e);
            return Response.error(ScheduleMessages.DELETE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response findScheduleById(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return Response.error(ScheduleMessages.SCHEDULE_ID_REQUIRED);
        }

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                Schedule schedule = repository.findScheduleById(em, scheduleId);
                if (schedule == null) {
                    return Response.error(ScheduleMessages.scheduleNotFoundById(scheduleId));
                }
                ScheduleDTO scheduleDTO = ScheduleMapper.INSTANCE.toDto(schedule);
                return Response.success(ScheduleMessages.FIND_BY_ID_SUCCESS, scheduleDTO);
            });
        } catch (Exception e) {
            log.error("Failed to find schedule by id: id={}", scheduleId, e);
            return Response.error(ScheduleMessages.FIND_BY_ID_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response findAllSchedules() {
        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                List<Schedule> schedules = repository.findAllSchedules(em);
                List<ScheduleDTO> scheduleDTOList = ScheduleMapper.INSTANCE.toDtoList(schedules);
                return Response.success(ScheduleMessages.FIND_ALL_SUCCESS, scheduleDTOList);
            });
        } catch (Exception e) {
            log.error("Failed to find all schedules", e);
            return Response.error(ScheduleMessages.FIND_ALL_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime,
            LocalDateTime toDateTime, String status) {
        if (fromDateTime != null && toDateTime != null && fromDateTime.isAfter(toDateTime)) {
            return Response.error(ScheduleMessages.START_TIME_AFTER_END_TIME);
        }

        String rawStatus = status;
        if (rawStatus != null && !rawStatus.isBlank()) {
            try {
                rawStatus = StatusSchedule.valueOf(rawStatus.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                String allowedStatuses = Arrays.stream(StatusSchedule.values())
                        .map(Enum::name)
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("");
                return Response.error(ScheduleMessages.INVALID_STATUS_PREFIX + allowedStatuses);
            }
        }

        try {
            String finalStatus = rawStatus;
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                List<Schedule> schedules = repository.searchSchedules(em, routeId, trainId, fromDateTime, toDateTime, finalStatus);
                List<ScheduleDTO> scheduleDTOList = ScheduleMapper.INSTANCE.toDtoList(schedules);
                return Response.success(ScheduleMessages.SEARCH_SUCCESS, scheduleDTOList);
            });
        } catch (Exception e) {
            log.error("Failed to search schedules: routeId={}, trainId={}", routeId, trainId, e);
            return Response.error(ScheduleMessages.SEARCH_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response findSchedulesByStationIds(String departureStationId, String destinationStationId) {
        if (departureStationId == null || departureStationId.isBlank()) {
            return Response.error(ScheduleMessages.DEPARTURE_STATION_REQUIRED);
        }
        if (destinationStationId == null || destinationStationId.isBlank()) {
            return Response.error(ScheduleMessages.DESTINATION_STATION_REQUIRED);
        }

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                List<Schedule> schedules = repository.findSchedulesByStationIds(em, departureStationId, destinationStationId);
                List<ScheduleDTO> scheduleDTOList = ScheduleMapper.INSTANCE.toDtoList(schedules);
                return Response.success(ScheduleMessages.FIND_BY_STATION_SUCCESS, scheduleDTOList);
            });
        } catch (Exception e) {
            log.error("Failed to find schedules by station ids: departureStationId={}, destinationStationId={}",
                    departureStationId, destinationStationId, e);
            return Response.error(ScheduleMessages.FIND_BY_STATION_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response publishSchedule(ScheduleLifecycleDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        String action = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";
        if (!"PUBLISH".equals(action)) {
            return Response.error(ScheduleMessages.ACTION_INVALID);
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Schedule existingSchedule = repository.findScheduleById(em, dto.getScheduleId());
                if (existingSchedule == null) {
                    return Response.error(ScheduleMessages.scheduleNotFoundById(dto.getScheduleId()));
                }
                if (existingSchedule.getStatus() != StatusSchedule.DRAFT) {
                    return Response.error(ScheduleMessages.ONLY_DRAFT_CAN_BE_PUBLISHED);
                }
                if (existingSchedule.getDepartureTime() == null
                        || !existingSchedule.getDepartureTime().isAfter(LocalDateTime.now())) {
                    return Response.error(ScheduleMessages.DEPARTURE_TIME_IN_PAST);
                }

                boolean hasUnpricedSeats = scheduleDetailRepository.existsUnpricedSeat(em, dto.getScheduleId());
                if (hasUnpricedSeats) {
                    return Response.error(ScheduleMessages.PRICE_NOT_CONFIGURED);
                }

                boolean updated = repository.updateScheduleStatus(em, dto.getScheduleId(), StatusSchedule.NOT_STARTED);
                if (!updated) {
                    return Response.error(ScheduleMessages.updateFailedById(dto.getScheduleId()));
                }

                log.info("Schedule published: id={}", dto.getScheduleId());
                return Response.success(ScheduleMessages.PUBLISH_SUCCESS, dto.getScheduleId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to publish schedule: id={}", dto.getScheduleId(), e);
            return Response.error(ScheduleMessages.PUBLISH_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response disableSchedule(ScheduleLifecycleDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        String action = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";
        if (!"DISABLE".equals(action)) {
            return Response.error(ScheduleMessages.ACTION_INVALID);
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Schedule existingSchedule = repository.findScheduleById(em, dto.getScheduleId());
                if (existingSchedule == null) {
                    return Response.error(ScheduleMessages.scheduleNotFoundById(dto.getScheduleId()));
                }

                StatusSchedule currentStatus = existingSchedule.getStatus();

                if (currentStatus == StatusSchedule.DRAFT) {
                    boolean deleted = repository.deleteSchedule(em, dto.getScheduleId());
                    if (!deleted) {
                        return Response.error(ScheduleMessages.deleteFailedById(dto.getScheduleId()));
                    }
                    log.info("Schedule DRAFT disabled (deleted): id={}", dto.getScheduleId());
                    return Response.success(ScheduleMessages.DISABLE_SUCCESS, dto.getScheduleId());
                }

                if (currentStatus == StatusSchedule.NOT_STARTED) {
                    long soldCount = repository.countSoldSeatsByScheduleId(em, dto.getScheduleId());
                    if (soldCount > 0) {
                        return Response.error(ScheduleMessages.TICKETS_SOLD_BLOCKED);
                    }
                    boolean updated = repository.updateScheduleStatus(em, dto.getScheduleId(), StatusSchedule.PAUSED);
                    if (!updated) {
                        return Response.error(ScheduleMessages.updateFailedById(dto.getScheduleId()));
                    }
                    log.info("Schedule NOT_STARTED disabled (paused): id={}", dto.getScheduleId());
                    return Response.success(ScheduleMessages.DISABLE_SUCCESS, dto.getScheduleId());
                }

                return Response.error(ScheduleMessages.WRONG_STATUS_DISABLE);
            });
        } catch (RuntimeException e) {
            log.error("Failed to disable schedule: id={}", dto.getScheduleId(), e);
            return Response.error(ScheduleMessages.DISABLE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response findScheduleDetails(ScheduleDetailPriceUpdateDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                Schedule schedule = repository.findScheduleById(em, dto.getScheduleId());
                if (schedule == null) {
                    return Response.error(ScheduleMessages.scheduleNotFoundById(dto.getScheduleId()));
                }
                List<ScheduleDetail> details = scheduleDetailRepository.findByScheduleIdWithSeat(em, dto.getScheduleId());
                Set<String> soldIds = scheduleDetailRepository.getSoldScheduleDetailIds(em, dto.getScheduleId());
                List<ScheduleDetailDTO> detailDtos = details.stream()
                        .map(detail -> toScheduleDetailDto(detail, soldIds))
                        .toList();
                return Response.success(ScheduleMessages.FIND_BY_ID_SUCCESS, detailDtos);
            });
        } catch (RuntimeException e) {
            log.error("Failed to find schedule details: scheduleId={}", dto.getScheduleId(), e);
            return Response.error(ScheduleMessages.FIND_BY_ID_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response updateScheduleDetailPrices(ScheduleDetailPriceUpdateDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }
        if (dto.getPricesByScheduleDetailId() == null || dto.getPricesByScheduleDetailId().isEmpty()) {
            return Response.error(ScheduleMessages.SCHEDULE_DETAIL_PRICE_REQUIRED);
        }

        Map<String, BigDecimal> prices = dto.getPricesByScheduleDetailId();
        boolean hasInvalidPrice = prices.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null
                        || entry.getKey().isBlank()
                        || entry.getValue() == null
                        || entry.getValue().compareTo(BigDecimal.ZERO) <= 0);
        if (hasInvalidPrice) {
            return Response.error(ScheduleMessages.SCHEDULE_DETAIL_PRICE_INVALID);
        }

        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Schedule schedule = repository.findScheduleById(em, dto.getScheduleId());
                if (schedule == null) {
                    return Response.error(ScheduleMessages.scheduleNotFoundById(dto.getScheduleId()));
                }
                if (schedule.getStatus() != StatusSchedule.DRAFT) {
                    return Response.error(ScheduleMessages.DRAFT_UPDATE_ONLY);
                }

                Set<String> requestedIds = new HashSet<>(prices.keySet());
                Set<String> existingIds = scheduleDetailRepository.findDetailIdsInSchedule(
                        em, dto.getScheduleId(), requestedIds);
                if (existingIds.size() != requestedIds.size()) {
                    requestedIds.removeAll(existingIds);
                    return Response.error(ScheduleMessages.scheduleDetailNotFound(requestedIds.iterator().next()));
                }

                scheduleDetailRepository.updatePrices(em, prices);
                log.info("Schedule detail prices updated: scheduleId={}, count={}", dto.getScheduleId(), prices.size());
                return Response.success(ScheduleMessages.UPDATE_PRICE_SUCCESS, prices.size());
            });
        } catch (RuntimeException e) {
            log.error("Failed to update schedule detail prices: scheduleId={}", dto.getScheduleId(), e);
            return Response.error(ScheduleMessages.UPDATE_PRICE_FAILED_PREFIX + e.getMessage());
        }
    }

    private Response validateActiveTrain(Train train, String trainId) {
        if (train == null) {
            return Response.error(ScheduleMessages.trainNotFoundById(trainId));
        }
        if (train.getStatus() != TrainStatus.ACTIVE) {
            return Response.error(ScheduleMessages.TRAIN_NOT_ACTIVE);
        }
        return null;
    }

    private Response validateActiveRoute(Route route, String routeId) {
        if (route == null) {
            return Response.error(ScheduleMessages.routeNotFoundById(routeId));
        }
        if (route.getStatus() != RouteStatus.ACTIVE) {
            return Response.error(ScheduleMessages.ROUTE_NOT_ACTIVE);
        }
        return null;
    }

    private Schedule buildDraftSchedule(Train train, Route route, LocalDateTime departure, LocalDateTime arrival) {
        return Schedule.builder()
                .train(train)
                .route(route)
                .departureTime(departure)
                .arrivalTime(arrival)
                .status(StatusSchedule.DRAFT)
                .build();
    }

    private ScheduleDetailDTO toScheduleDetailDto(ScheduleDetail detail, Set<String> soldIds) {
        return ScheduleDetailDTO.builder()
                .id(detail.getId())
                .seatPrice(detail.getPriceSeat() != null ? detail.getPriceSeat().doubleValue() : 0)
                .scheduleId(detail.getSchedule() != null ? detail.getSchedule().getId() : null)
                .seatId(detail.getSeat() != null ? detail.getSeat().getId() : null)
                .routeStopId(detail.getRouteStop() != null ? detail.getRouteStop().getId() : null)
                .segmentDepartureStationId(detail.getSegmentDepartureStation() != null
                        ? detail.getSegmentDepartureStation().getId()
                        : null)
                .segmentDepartureStationName(detail.getSegmentDepartureStation() != null
                        ? detail.getSegmentDepartureStation().getName()
                        : null)
                .segmentDestinationStationId(detail.getSegmentDestinationStation() != null
                        ? detail.getSegmentDestinationStation().getId()
                        : null)
                .segmentDestinationStationName(detail.getSegmentDestinationStation() != null
                        ? detail.getSegmentDestinationStation().getName()
                        : null)
                .segmentDepartureOrder(detail.getSegmentDepartureOrder())
                .segmentDestinationOrder(detail.getSegmentDestinationOrder())
                .seatNumber(detail.getSeat() != null ? detail.getSeat().getNumber() : 0)
                .seatType(detail.getSeat() != null ? detail.getSeat().getType() : null)
                .carriageNumber(detail.getSeat() != null && detail.getSeat().getCarriage() != null
                        ? detail.getSeat().getCarriage().getNumber()
                        : 0)
                .sold(soldIds.contains(detail.getId()))
                .build();
    }
}
