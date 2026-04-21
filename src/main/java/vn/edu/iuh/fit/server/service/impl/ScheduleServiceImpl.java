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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class ScheduleServiceImpl implements ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    private final ScheduleRepository repository = new ScheduleRepositoryImpl();

    @Override
    public Response createSchedule(ScheduleCreateDTO scheduleDTO) {
        List<String> errors = ValidationUtils.validate(scheduleDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        try {
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
            log.info("Schedule created: id={}, trainId={}", savedSchedule.getId(), scheduleDTO.getTrainId());
            return Response.success("Tạo lịch trình thành công", savedSchedule.getId());
        } catch (Exception e) {
            log.error("Failed to create schedule: trainId={}, routeId={}", scheduleDTO.getTrainId(), scheduleDTO.getRouteId(), e);
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
            List<Schedule> schedules = repository.filterSchedules(filter);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.toDtoList(schedules);
            return Response.success("Lọc lịch trình thành công", scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to filter schedules", e);
            return Response.error("Lỗi khi lọc lịch trình: " + e.getMessage());
        }
    }

    @Override
    public Response updateSchedule(ScheduleDTO scheduleDTO) {
        List<String> errors = ValidationUtils.validate(scheduleDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        try {
            Schedule existingSchedule = repository.findScheduleById(scheduleDTO.getId());
            if (existingSchedule == null) {
                return Response.error("Không tìm thấy lịch trình: id=" + scheduleDTO.getId());
            }
            if (existingSchedule.getStatus() != StatusSchedule.DRAFT) {
                return Response.error("Chỉ được phép sửa lịch trình khi đang ở trạng thái Nháp");
            }

            Schedule scheduleToUpdate = ScheduleMapper.toEntity(scheduleDTO);
            scheduleToUpdate.setId(existingSchedule.getId());
            scheduleToUpdate.setStatus(existingSchedule.getStatus());

            boolean updated = repository.updateSchedule(scheduleToUpdate);
            if (!updated) {
                return Response.error("Không thể cập nhật lịch trình: id=" + scheduleDTO.getId());
            }

            Schedule updatedSchedule = repository.findScheduleById(scheduleDTO.getId());
            ScheduleDTO updatedScheduleDTO = ScheduleMapper.toDto(updatedSchedule);
            log.info("Schedule updated: id={}", scheduleDTO.getId());
            return Response.success("Cập nhật lịch trình thành công", updatedScheduleDTO);
        } catch (Exception e) {
            log.error("Failed to update schedule: id={}", scheduleDTO.getId(), e);
            return Response.error("Lỗi khi cập nhật lịch trình: " + e.getMessage());
        }
    }

    @Override
    public Response deleteSchedule(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return Response.error("Mã lịch trình không được để trống");
        }

        try {
            Schedule existingSchedule = repository.findScheduleById(scheduleId);
            if (existingSchedule == null) {
                return Response.error("Không tìm thấy lịch trình: id=" + scheduleId);
            }
            if (existingSchedule.getStatus() != StatusSchedule.DRAFT) {
                return Response.error("Chỉ được phép xoá lịch trình khi đang ở trạng thái Nháp");
            }

            boolean deleted = repository.deleteSchedule(scheduleId);
            if (!deleted) {
                return Response.error("Không thể xoá lịch trình: id=" + scheduleId);
            }

            log.info("Schedule deleted: id={}", scheduleId);
            return Response.success("Xoá lịch trình thành công", scheduleId);
        } catch (Exception e) {
            log.error("Failed to delete schedule: id={}", scheduleId, e);
            return Response.error("Lỗi khi xoá lịch trình: " + e.getMessage());
        }
    }

    @Override
    public Response findScheduleById(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return Response.error("Mã lịch trình không được để trống");
        }

        try {
            Schedule schedule = repository.findScheduleById(scheduleId);
            if (schedule == null) {
                return Response.error("Không tìm thấy lịch trình: id=" + scheduleId);
            }

            ScheduleDTO scheduleDTO = ScheduleMapper.toDto(schedule);
            return Response.success("Lấy lịch trình thành công", scheduleDTO);
        } catch (Exception e) {
            log.error("Failed to find schedule by id: id={}", scheduleId, e);
            return Response.error("Lỗi khi tìm lịch trình: " + e.getMessage());
        }
    }

    @Override
    public Response findAllSchedules() {
        try {
            List<Schedule> schedules = repository.findAllSchedules();
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.toDtoList(schedules);
            return Response.success("Lấy danh sách lịch trình thành công", scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to find all schedules", e);
            return Response.error("Lỗi khi lấy danh sách lịch trình: " + e.getMessage());
        }
    }

    @Override
    public Response searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime,
            LocalDateTime toDateTime, String status) {
        if (fromDateTime != null && toDateTime != null && fromDateTime.isAfter(toDateTime)) {
            return Response.error("Thời gian bắt đầu không được lớn hơn thời gian kết thúc");
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
                return Response.error("Trạng thái không hợp lệ. Giá trị hợp lệ: " + allowedStatuses);
            }
        }

        try {
            List<Schedule> schedules = repository.searchSchedules(routeId, trainId, fromDateTime, toDateTime, normalizedStatus);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.toDtoList(schedules);
            return Response.success("Tìm kiếm lịch trình thành công", scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to search schedules: routeId={}, trainId={}", routeId, trainId, e);
            return Response.error("Lỗi khi tìm kiếm lịch trình: " + e.getMessage());
        }
    }

    @Override
    public Response findSchedulesByStationIds(String departureStationId, String destinationStationId) {
        if (departureStationId == null || departureStationId.isBlank()) {
            return Response.error("Ga đi không được để trống");
        }
        if (destinationStationId == null || destinationStationId.isBlank()) {
            return Response.error("Ga đến không được để trống");
        }

        try {
            List<Schedule> schedules = repository.findSchedulesByStationIds(departureStationId, destinationStationId);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.toDtoList(schedules);
            return Response.success("Lấy lịch trình theo ga thành công", scheduleDTOList);
        } catch (Exception e) {
            log.error("Failed to find schedules by station ids: departureStationId={}, destinationStationId={}",
                    departureStationId, destinationStationId, e);
            return Response.error("Lỗi khi lấy lịch trình theo ga: " + e.getMessage());
        }
    }
}

