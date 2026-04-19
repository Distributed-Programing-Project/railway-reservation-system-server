package vn.edu.iuh.fit.server.service.impl;

import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.service.ScheduleService;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduleServiceImpl implements ScheduleService {
    @Override
    public boolean createSchedule(Schedule schedule) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean updateSchedule(Schedule schedule) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean deleteSchedule(String scheduleId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Schedule findScheduleById(String scheduleId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Schedule> findAllSchedules() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Schedule> searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime, String status) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Schedule> findSchedulesByStationIds(String departureStationId, String destinationStationId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

