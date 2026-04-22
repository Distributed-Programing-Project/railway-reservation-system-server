package vn.edu.iuh.fit.server.service.impl;

import vn.edu.iuh.fit.common.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.common.dto.ScheduleUpdateDTO;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.common.constant.StatusSchedule;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleRepositoryImpl;
import vn.edu.iuh.fit.server.mapper.ScheduleMapper;
import vn.edu.iuh.fit.common.message.ScheduleMessages;
import vn.edu.iuh.fit.server.util.ValidationUtils;
import vn.edu.iuh.fit.server.util.JPAUtils;
import vn.edu.iuh.fit.common.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class ScheduleServiceImpl implements ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    private final ScheduleRepository repository = new ScheduleRepositoryImpl();
    private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();


    @Override
    public Response createSchedule(ScheduleCreateDTO scheduleDTO) {
        List<String> errors = ValidationUtils.validate(scheduleDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Employee requester = findRequester(scheduleDTO.getRequestEmployeeId());
        if (requester == null) {
            return Response.error("Không tìm thấy nhân viên: id=" + scheduleDTO.getRequestEmployeeId());
        }
        if (!Boolean.TRUE.equals(requester.getIsManager())) {
            return Response.error("Bạn không có quyền thực hiện thao tác này");
        }

        if (scheduleDTO.getDepartureTime().isBefore(LocalDateTime.now().plusDays(1))) {
            return Response.error(ScheduleMessages.DEPARTURE_TIME_MIN_ONE_DAY);
        }
        if (scheduleDTO.getArrivalTime().isBefore(scheduleDTO.getDepartureTime())) {
            return Response.error(ScheduleMessages.ARRIVAL_TIME_INVALID);
        }

        EntityManager em = JPAUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Train train = Train.builder().id(scheduleDTO.getTrainId()).build();
            Route route = Route.builder().id(scheduleDTO.getRouteId()).build();
            Schedule schedule = Schedule.builder()
                    .train(train)
                    .route(route)
                    .departureTime(scheduleDTO.getDepartureTime())
                    .arrivalTime(scheduleDTO.getArrivalTime())
                    .status(StatusSchedule.DRAFT)
                    .build();

            Schedule savedSchedule = repository.createScheduleWithDetails(em, schedule, scheduleDTO.getTrainId());
            tx.commit();
            log.info("Schedule created: id={}, trainId={}", savedSchedule.getId(), scheduleDTO.getTrainId());
            return Response.success(ScheduleMessages.CREATE_SUCCESS, savedSchedule.getId());
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to create schedule: trainId={}, routeId={}", scheduleDTO.getTrainId(), scheduleDTO.getRouteId(), e);
            return Response.error(ScheduleMessages.CREATE_FAILED_PREFIX + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Response updateSchedule(ScheduleUpdateDTO scheduleUpdateDTO) {
        List<String> errors = ValidationUtils.validate(scheduleUpdateDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Employee requester = findRequester(scheduleUpdateDTO.getRequestEmployeeId());
        if (requester == null) {
            return Response.error(ScheduleMessages.employeeNotFoundById(scheduleUpdateDTO.getRequestEmployeeId()));
        }
        if (!Boolean.TRUE.equals(requester.getIsManager())) {
            return Response.error(ScheduleMessages.UNAUTHORIZED);
        }

        EntityManager em = JPAUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Schedule existingSchedule = repository.findScheduleById(em, scheduleUpdateDTO.getScheduleId());
            if (existingSchedule == null) {
                return Response.error(ScheduleMessages.scheduleNotFoundById(scheduleUpdateDTO.getScheduleId()));
            }
            if (existingSchedule.getStatus() != StatusSchedule.DRAFT) {
                return Response.error(ScheduleMessages.DRAFT_UPDATE_ONLY);
            }

            Schedule scheduleToUpdate = ScheduleMapper.INSTANCE.toEntityForUpdate(scheduleUpdateDTO);
            boolean updated = repository.updateSchedule(em, scheduleToUpdate);
            if (!updated) {
                return Response.error(ScheduleMessages.updateFailedById(scheduleUpdateDTO.getScheduleId()));
            }

            tx.commit();
            log.info("Schedule updated: id={}", scheduleUpdateDTO.getScheduleId());
            return Response.success(ScheduleMessages.UPDATE_SUCCESS, scheduleUpdateDTO.getScheduleId());
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to update schedule: id={}", scheduleUpdateDTO.getScheduleId(), e);
            return Response.error(ScheduleMessages.UPDATE_FAILED_PREFIX + e.getMessage());
        } finally {
            em.close();
        }
    }

    private Employee findRequester(String employeeId) {
        EntityManager em = JPAUtils.getEntityManager();
        try {
            return employeeRepository.findEmployeeById(em, employeeId);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    public Response filterSchedules(ScheduleFilterDTO filter) {
        List<String> errors = ValidationUtils.validate(filter);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        EntityManager em = JPAUtils.getEntityManager();
        try {
            List<Schedule> schedules = repository.filterSchedules(em, filter);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.INSTANCE.toDtoList(schedules);
            return Response.success(ScheduleMessages.FILTER_SUCCESS, scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to filter schedules", e);
            return Response.error(ScheduleMessages.FILTER_FAILED_PREFIX + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Response deleteSchedule(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return Response.error(ScheduleMessages.SCHEDULE_ID_REQUIRED);
        }

        EntityManager em = JPAUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
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

            tx.commit();
            log.info("Schedule deleted: id={}", scheduleId);
            return Response.success(ScheduleMessages.DELETE_SUCCESS, scheduleId);
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete schedule: id={}", scheduleId, e);
            return Response.error(ScheduleMessages.DELETE_FAILED_PREFIX + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Response findScheduleById(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return Response.error(ScheduleMessages.SCHEDULE_ID_REQUIRED);
        }

        EntityManager em = JPAUtils.getEntityManager();
        try {
            Schedule schedule = repository.findScheduleById(em, scheduleId);
            if (schedule == null) {
                return Response.error(ScheduleMessages.scheduleNotFoundById(scheduleId));
            }

            ScheduleDTO scheduleDTO = ScheduleMapper.INSTANCE.toDto(schedule);
            return Response.success(ScheduleMessages.FIND_BY_ID_SUCCESS, scheduleDTO);
        } catch (Exception e) {
            log.error("Failed to find schedule by id: id={}", scheduleId, e);
            return Response.error(ScheduleMessages.FIND_BY_ID_FAILED_PREFIX + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Response findAllSchedules() {
        EntityManager em = JPAUtils.getEntityManager();
        try {
            List<Schedule> schedules = repository.findAllSchedules(em);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.INSTANCE.toDtoList(schedules);
            return Response.success(ScheduleMessages.FIND_ALL_SUCCESS, scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to find all schedules", e);
            return Response.error(ScheduleMessages.FIND_ALL_FAILED_PREFIX + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Response searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime,
            LocalDateTime toDateTime, String status) {
        if (fromDateTime != null && toDateTime != null && fromDateTime.isAfter(toDateTime)) {
            return Response.error(ScheduleMessages.START_TIME_AFTER_END_TIME);
        }

        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                normalizedStatus = StatusSchedule.valueOf(status.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                String allowedStatuses = Arrays.stream(StatusSchedule.values())
                        .map(Enum::name)
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("");
                return Response.error(ScheduleMessages.INVALID_STATUS_PREFIX + allowedStatuses);
            }
        }

        EntityManager em = JPAUtils.getEntityManager();
        try {
            List<Schedule> schedules = repository.searchSchedules(em, routeId, trainId, fromDateTime, toDateTime, normalizedStatus);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.INSTANCE.toDtoList(schedules);
            return Response.success(ScheduleMessages.SEARCH_SUCCESS, scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to search schedules: routeId={}, trainId={}", routeId, trainId, e);
            return Response.error(ScheduleMessages.SEARCH_FAILED_PREFIX + e.getMessage());
        } finally {
            em.close();
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

        EntityManager em = JPAUtils.getEntityManager();
        try {
            List<Schedule> schedules = repository.findSchedulesByStationIds(em, departureStationId, destinationStationId);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.INSTANCE.toDtoList(schedules);
            return Response.success(ScheduleMessages.FIND_BY_STATION_SUCCESS, scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to find schedules by station ids: departureStationId={}, destinationStationId={}",
                    departureStationId, destinationStationId, e);
            return Response.error(ScheduleMessages.FIND_BY_STATION_FAILED_PREFIX + e.getMessage());
        } finally {
            em.close();
        }
    }
}


