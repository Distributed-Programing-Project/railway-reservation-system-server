package vn.edu.iuh.fit.server.service.impl;

import vn.edu.iuh.fit.server.dto.ScheduleDTO;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.service.ScheduleService;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.repository.impl.ScheduleRepositoryImpl;
import vn.edu.iuh.fit.server.mapper.ScheduleMapper;

import vn.edu.iuh.fit.common.response.Response;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.List;

public class ScheduleServiceImpl implements ScheduleService {
    
    private final ScheduleRepository repository = new ScheduleRepositoryImpl();

    @Override
    public Response createSchedule(ScheduleDTO scheduleDTO) {
        throw new UnsupportedOperationException("Not implemented yet");
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
    public Response searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime, String status) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Response findSchedulesByStationIds(String departureStationId, String destinationStationId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Response filterSchedules(ScheduleFilterDTO filter) {
        try {
            if (filter.getFromDate() != null && filter.getToDate() != null && filter.getFromDate().isAfter(filter.getToDate())) {
                return Response.error("Từ ngày không được lớn hơn Đến ngày.");
            }

            List<Schedule> schedules = repository.filterSchedules(filter);
            List<ScheduleDTO> scheduleDTOList = ScheduleMapper.toDtoList(schedules);
            return Response.success("Lọc lịch trình thành công", scheduleDTOList);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Lỗi khi lọc lịch trình: " + e.getMessage());
        }
    }
}
