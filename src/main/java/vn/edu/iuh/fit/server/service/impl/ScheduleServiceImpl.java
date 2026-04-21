package vn.edu.iuh.fit.server.service.impl;

import vn.edu.iuh.fit.server.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.server.dto.ScheduleDTO;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.constant.StatusSchedule;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleRepositoryImpl;
import vn.edu.iuh.fit.server.mapper.ScheduleMapper;
import vn.edu.iuh.fit.server.util.JPAUtils;
import vn.edu.iuh.fit.server.util.ValidationUtils;

import vn.edu.iuh.fit.common.response.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
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
            return Response.error("Ngày khởi hành phải cách ít nhất 1 ngày so với hôm nay");
        }
        if (scheduleDTO.getArrivalTime().isBefore(scheduleDTO.getDepartureTime())) {
            return Response.error("Ngày giờ đến dự kiến không được nhỏ hơn giờ khởi hành");
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
            return Response.success("Tạo lịch trình thành công", savedSchedule.getId());
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to create schedule: trainId={}, routeId={}", scheduleDTO.getTrainId(), scheduleDTO.getRouteId(), e);
            return Response.error("Lỗi khi tạo lịch trình: " + e.getMessage());
        } finally {
            if (tx.isActive()) tx.rollback();
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
            if (filter.getFromDate() != null && filter.getToDate() != null
                    && filter.getFromDate().isAfter(filter.getToDate())) {
                return Response.error("Từ ngày không được lớn hơn Đến ngày.");
            }

            List<Schedule> schedules = repository.filterSchedules(em, filter);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.toDtoList(schedules);
            return Response.success("Lọc lịch trình thành công", scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to filter schedules", e);
            return Response.error("Lỗi khi lọc lịch trình: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Response updateSchedule(ScheduleDTO scheduleDTO) {
        return Response.error("Chưa hỗ trợ");
    }

    @Override
    public Response deleteSchedule(String scheduleId) {
        return Response.error("Chưa hỗ trợ");
    }

    @Override
    public Response findScheduleById(String scheduleId) {
        return Response.error("Chưa hỗ trợ");
    }

    @Override
    public Response findAllSchedules() {
        return Response.error("Chưa hỗ trợ");
    }

    @Override
    public Response searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime,
            LocalDateTime toDateTime, String status) {
        return Response.error("Chưa hỗ trợ");
    }

    @Override
    public Response findSchedulesByStationIds(String departureStationId, String destinationStationId) {
        return Response.error("Chưa hỗ trợ");
    }
}

