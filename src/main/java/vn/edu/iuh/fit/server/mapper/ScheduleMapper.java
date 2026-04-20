package vn.edu.iuh.fit.server.mapper;

import vn.edu.iuh.fit.server.dto.ScheduleDTO;
import vn.edu.iuh.fit.server.model.Schedule;

import java.util.List;

public class ScheduleMapper {

    private static final GenericDataMapper mapper = new JacksonDataMapper();

    public static ScheduleDTO toDto(Schedule schedule) {
        if (schedule == null) return null;
        ScheduleDTO dto = mapper.toObject(mapper.toMap(schedule), ScheduleDTO.class);
        
        if (schedule.getTrain() != null) {
            dto.setTrainId(schedule.getTrain().getId());
            dto.setTrainName(schedule.getTrain().getTrainCode());
        }
        
        if (schedule.getRoute() != null) {
            dto.setRouteId(schedule.getRoute().getId());
            dto.setRouteCode(schedule.getRoute().getRouteCode());
            if (schedule.getRoute().getDepartureStation() != null) {
                dto.setDepartureStationName(schedule.getRoute().getDepartureStation().getName());
            }
            if (schedule.getRoute().getDestinationStation() != null) {
                dto.setDestinationStationName(schedule.getRoute().getDestinationStation().getName());
            }
        }
        
        return dto;
    }

    public static Schedule toEntity(ScheduleDTO dto) {
        if (dto == null) return null;
        return mapper.toObject(mapper.toMap(dto), Schedule.class);
    }

    public static List<ScheduleDTO> toDtoList(List<Schedule> schedules) {
        return schedules.stream()
                .map(ScheduleMapper::toDto)
                .toList();
    }
}
