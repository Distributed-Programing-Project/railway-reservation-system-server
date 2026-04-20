package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.server.dto.ScheduleDTO;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.server.model.Schedule;

import java.time.LocalDateTime;
import java.util.List;

import vn.edu.iuh.fit.common.response.Response;

public interface ScheduleService {
    Response createSchedule(ScheduleDTO scheduleDTO);

    Response updateSchedule(ScheduleDTO scheduleDTO);

    Response deleteSchedule(String scheduleId);

    Response findScheduleById(String scheduleId);

    Response findAllSchedules();

    Response searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime,
            String status);

    Response findSchedulesByStationIds(String departureStationId, String destinationStationId);

    Response filterSchedules(ScheduleFilterDTO filter);
}
