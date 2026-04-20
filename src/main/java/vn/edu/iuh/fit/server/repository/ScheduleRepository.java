package vn.edu.iuh.fit.server.repository;

import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository {
    Schedule createScheduleWithDetails(Schedule schedule, String trainId);

    boolean updateSchedule(Schedule schedule);

    boolean deleteSchedule(String scheduleId);

    Schedule findScheduleById(String scheduleId);

    List<Schedule> findAllSchedules();

    List<Schedule> searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime, String status);

    List<Schedule> findSchedulesByStationIds(String departureStationId, String destinationStationId);

    List<Schedule> filterSchedules(ScheduleFilterDTO filter);
}

