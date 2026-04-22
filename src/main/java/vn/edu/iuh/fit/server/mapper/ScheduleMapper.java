package vn.edu.iuh.fit.server.mapper;

import vn.edu.iuh.fit.common.dto.ScheduleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleUpdateDTO;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.Train;

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
        Schedule schedule = mapper.toObject(mapper.toMap(dto), Schedule.class);

        if (dto.getTrainId() != null && !dto.getTrainId().isBlank()) {
            Train train = new Train();
            train.setId(dto.getTrainId());
            schedule.setTrain(train);
        }

        if (dto.getRouteId() != null && !dto.getRouteId().isBlank()) {
            Route route = new Route();
            route.setId(dto.getRouteId());
            schedule.setRoute(route);
        }

        return schedule;
    }

    public static Schedule toEntityForUpdate(ScheduleUpdateDTO dto) {
        if (dto == null) return null;
        Train train = new Train();
        train.setId(dto.getTrainId());
        Route route = new Route();
        route.setId(dto.getRouteId());
        return Schedule.builder()
                .id(dto.getScheduleId())
                .train(train)
                .route(route)
                .departureTime(dto.getDepartureTime())
                .arrivalTime(dto.getArrivalTime())
                .build();
    }

    public static List<ScheduleDTO> toDtoList(List<Schedule> schedules) {
        return schedules.stream()
                .map(ScheduleMapper::toDto)
                .toList();
    }
}
