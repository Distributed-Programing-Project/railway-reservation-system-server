package vn.edu.iuh.fit.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import vn.edu.iuh.fit.common.dto.ScheduleDTO;
import vn.edu.iuh.fit.common.dto.ScheduleUpdateDTO;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.Train;

import java.util.List;

@Mapper
public interface ScheduleMapper {
    ScheduleMapper INSTANCE = Mappers.getMapper(ScheduleMapper.class);

    @Mapping(source = "train.id", target = "trainId")
    @Mapping(source = "train.trainCode", target = "trainName")
    @Mapping(source = "route.id", target = "routeId")
    @Mapping(source = "route.routeCode", target = "routeCode")
    @Mapping(source = "route.departureStation.name", target = "departureStationName")
    @Mapping(source = "route.destinationStation.name", target = "destinationStationName")
    ScheduleDTO toDto(Schedule schedule);

    List<ScheduleDTO> toDtoList(List<Schedule> schedules);

    default Schedule toEntity(ScheduleDTO dto) {
        if (dto == null) return null;
        return Schedule.builder()
                .id(dto.getId())
                .departureTime(dto.getDepartureTime())
                .arrivalTime(dto.getArrivalTime())
                .status(dto.getStatus())
                .train(dto.getTrainId() != null ? Train.builder().id(dto.getTrainId()).build() : null)
                .route(dto.getRouteId() != null ? Route.builder().id(dto.getRouteId()).build() : null)
                .build();
    }

    @Mapping(target = "id", source = "scheduleId")
    @Mapping(target = "train", expression = "java(Train.builder().id(dto.getTrainId()).build())")
    @Mapping(target = "route", expression = "java(Route.builder().id(dto.getRouteId()).build())")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "scheduleDetails", ignore = true)
    Schedule toEntityForUpdate(ScheduleUpdateDTO dto);
}
