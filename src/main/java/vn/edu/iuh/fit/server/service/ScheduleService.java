package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.server.model.Schedule;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleService {
    boolean createSchedule(Schedule schedule);

    boolean updateSchedule(Schedule schedule);

    boolean deleteSchedule(String scheduleId);

    Schedule findScheduleById(String scheduleId);

    List<Schedule> findAllSchedules();

    List<Schedule> searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime, String status);

    List<Schedule> findSchedulesByStationIds(String departureStationId, String destinationStationId);
}

