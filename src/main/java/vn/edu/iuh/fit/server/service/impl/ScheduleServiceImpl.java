package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.constant.StatusSchedule;
import vn.edu.iuh.fit.common.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.common.dto.ScheduleLifecycleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleUpdateDTO;
import vn.edu.iuh.fit.common.message.ScheduleMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.mapper.ScheduleMapper;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
        if (scheduleDTO.getArrivalTime().isBefore(scheduleDTO.getDepartureTime())) {
            return Response.error(ScheduleMessages.ARRIVAL_TIME_INVALID);
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Train train = em.getReference(Train.class, scheduleDTO.getTrainId());
                Route route = em.getReference(Route.class, scheduleDTO.getRouteId());
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
                        && scheduleUpdateDTO.getDepartureTime().isBefore(LocalDateTime.now().plusDays(1))) {
                    return Response.error(ScheduleMessages.DEPARTURE_TIME_MIN_ONE_DAY);
                }
                LocalDateTime departure = scheduleUpdateDTO.getDepartureTime() != null
                        ? scheduleUpdateDTO.getDepartureTime()
                        : existingSchedule.getDepartureTime();
                if (scheduleUpdateDTO.getArrivalTime() != null
                        && scheduleUpdateDTO.getArrivalTime().isBefore(departure)) {
                    return Response.error(ScheduleMessages.ARRIVAL_TIME_INVALID);
                }

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

                boolean hasUnpricedSeats = scheduleDetailRepository.existsUnpricedSeat(em, dto.getScheduleId());
                if (hasUnpricedSeats) {
                    return Response.error(ScheduleMessages.PRICE_NOT_CONFIGURED);
                }

                boolean updated = repository.updateScheduleStatus(em, dto.getScheduleId(), StatusSchedule.NOT_STARTED);
                if (!updated) {
                    return Response.error(ScheduleMessages.UPDATE_FAILED_BY_ID);
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
                        return Response.error(ScheduleMessages.UPDATE_FAILED_BY_ID);
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
}
