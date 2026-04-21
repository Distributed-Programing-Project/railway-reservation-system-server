package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository {
    Schedule createScheduleWithDetails(EntityManager em, Schedule schedule, String trainId);

    boolean updateSchedule(EntityManager em, Schedule schedule);

    boolean deleteSchedule(EntityManager em, String scheduleId);

    Schedule findScheduleById(EntityManager em, String scheduleId);

    List<Schedule> findAllSchedules(EntityManager em);

    List<Schedule> searchSchedules(EntityManager em, String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime, String status);

    List<Schedule> findSchedulesByStationIds(EntityManager em, String departureStationId, String destinationStationId);

    List<Schedule> filterSchedules(EntityManager em, ScheduleFilterDTO filter);
}

