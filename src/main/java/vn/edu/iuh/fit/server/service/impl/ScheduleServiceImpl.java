package vn.edu.iuh.fit.server.service.impl;

import vn.edu.iuh.fit.server.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.server.dto.ScheduleDTO;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.constant.StatusSchedule;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.repository.impl.ScheduleRepositoryImpl;
import vn.edu.iuh.fit.server.mapper.ScheduleMapper;
import vn.edu.iuh.fit.server.util.ValidationUtils;

import vn.edu.iuh.fit.common.response.Response;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository repository = new ScheduleRepositoryImpl();

    @Override
    public Response createSchedule(ScheduleCreateDTO scheduleDTO) {
        List<String> errors = ValidationUtils.validate(scheduleDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        try {
            // Business Rule Validation
            if (scheduleDTO.getDepartureTime().isBefore(LocalDateTime.now())) {
                return Response.error("Ngày hoặc giờ khởi hành không được ở quá khứ");
            }
            if (scheduleDTO.getDepartureTime().isBefore(LocalDateTime.now().plusDays(1))) {
                return Response.error("Ngày khởi hành phải cách ít nhất 1 ngày so với hôm nay");
            }
            if (scheduleDTO.getArrivalTime() != null
                    && scheduleDTO.getArrivalTime().isBefore(scheduleDTO.getDepartureTime())) {
                return Response.error("Ngày giờ đến dự kiến không được nhỏ hơn giờ khởi hành");
            }

            Train train = new Train();
            train.setId(scheduleDTO.getTrainId());

            Route route = new Route();
            route.setId(scheduleDTO.getRouteId());

            Schedule schedule = Schedule.builder()
                    .train(train)
                    .route(route)
                    .departureTime(scheduleDTO.getDepartureTime())
                    .arrivalTime(scheduleDTO.getArrivalTime())
                    .status(StatusSchedule.DRAFT)
                    .build();

            Schedule savedSchedule = repository.createScheduleWithDetails(schedule, scheduleDTO.getTrainId());
            return Response.success("Tạo lịch trình thành công", savedSchedule.getId());
        } catch (Exception e) {
            return Response.error("Lỗi khi tạo lịch trình: " + e.getMessage());
        }
    }

    @Override
    public Response filterSchedules(ScheduleFilterDTO filter) {
        List<String> errors = ValidationUtils.validate(filter);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        try {
            if (filter.getFromDate() != null && filter.getToDate() != null
                    && filter.getFromDate().isAfter(filter.getToDate())) {
                return Response.error("Từ ngày không được lớn hơn Đến ngày.");
            }

            List<Schedule> schedules = repository.filterSchedules(filter);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.toDtoList(schedules);
            return Response.success("Lọc lịch trình thành công", scheduleDTOList);
        } catch (Exception e) {
            return Response.error("Lỗi khi lọc lịch trình: " + e.getMessage());
        }
    }

    @Override
    public Response updateSchedule(ScheduleDTO scheduleDTO) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Response deleteSchedule(String scheduleId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Response findScheduleById(String scheduleId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Response findAllSchedules() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Response searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime,
            LocalDateTime toDateTime, String status) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Response findSchedulesByStationIds(String departureStationId, String destinationStationId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

