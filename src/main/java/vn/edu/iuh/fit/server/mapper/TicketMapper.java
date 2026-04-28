package vn.edu.iuh.fit.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import vn.edu.iuh.fit.common.dto.ReturnTicketTicketDTO;
import vn.edu.iuh.fit.server.model.Ticket;

import java.util.List;

@Mapper
public interface TicketMapper {
    TicketMapper INSTANCE = Mappers.getMapper(TicketMapper.class);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "scheduleDetailId", source = "scheduleDetail.id")
    @Mapping(target = "scheduleId", source = "scheduleDetail.schedule.id")
    @Mapping(target = "departureTime", source = "scheduleDetail.schedule.departureTime")
    @Mapping(target = "trainCode", expression = "java(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSchedule() != null && ticket.getScheduleDetail().getSchedule().getTrain() != null ? ticket.getScheduleDetail().getSchedule().getTrain().getTrainCode() : null)")
    @Mapping(target = "departureStation", expression = "java(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSchedule() != null && ticket.getScheduleDetail().getSchedule().getRoute() != null && ticket.getScheduleDetail().getSchedule().getRoute().getDepartureStation() != null ? ticket.getScheduleDetail().getSchedule().getRoute().getDepartureStation().getName() : null)")
    @Mapping(target = "destinationStation", expression = "java(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSchedule() != null && ticket.getScheduleDetail().getSchedule().getRoute() != null && ticket.getScheduleDetail().getSchedule().getRoute().getDestinationStation() != null ? ticket.getScheduleDetail().getSchedule().getRoute().getDestinationStation().getName() : null)")
    @Mapping(target = "carriageName", expression = "java(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSeat() != null && ticket.getScheduleDetail().getSeat().getCarriage() != null ? String.valueOf(ticket.getScheduleDetail().getSeat().getCarriage().getNumber()) : null)")
    @Mapping(target = "seatNumber", expression = "java(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getSeat() != null ? String.valueOf(ticket.getScheduleDetail().getSeat().getNumber()) : null)")
    @Mapping(target = "ticketPrice", expression = "java(ticket.getScheduleDetail() != null && ticket.getScheduleDetail().getPriceSeat() != null ? ticket.getScheduleDetail().getPriceSeat().doubleValue() : 0.0)")
    @Mapping(target = "passengerName", source = "passengerName")
    @Mapping(target = "passengerIdCard", source = "passengerIdCard")
    ReturnTicketTicketDTO toReturnTicketDto(Ticket ticket);

    List<ReturnTicketTicketDTO> toReturnTicketDtoList(List<Ticket> tickets);
}
