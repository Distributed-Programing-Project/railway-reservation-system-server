package vn.edu.iuh.fit.server.service;

import java.time.LocalDateTime;

import vn.edu.iuh.fit.common.dto.ScheduleCreateDTO;
import vn.edu.iuh.fit.common.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.common.dto.ScheduleUpdateDTO;
import vn.edu.iuh.fit.common.response.Response;

public interface ScheduleService {
    Response createSchedule(ScheduleCreateDTO scheduleDTO);

    Response updateSchedule(ScheduleUpdateDTO scheduleUpdateDTO);

    Response deleteSchedule(String scheduleId);

    Response findScheduleById(String scheduleId);

    Response findAllSchedules();

    Response searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime,
            String status);

    Response findSchedulesByStationIds(String departureStationId, String destinationStationId);

    Response filterSchedules(ScheduleFilterDTO filter);
}
