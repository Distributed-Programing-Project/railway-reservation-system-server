package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.StatusSchedule;
import vn.edu.iuh.fit.common.message.ScheduleMessages;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDTO implements Serializable {
    @NotBlank(message = ScheduleMessages.SCHEDULE_ID_REQUIRED)
    private String id;

    @NotNull(message = ScheduleMessages.DEPARTURE_TIME_REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime arrivalTime;

    private StatusSchedule status;

    @NotBlank(message = ScheduleMessages.TRAIN_ID_REQUIRED)
    private String trainId;

    private String trainName;

    @NotBlank(message = ScheduleMessages.ROUTE_ID_REQUIRED)
    private String routeId;

    private String routeCode;
    private String departureStationName;
    private String destinationStationName;

    @AssertTrue(message = ScheduleMessages.DEPARTURE_TIME_MIN_ONE_DAY)
    public boolean isDepartureTimeAtLeastOneDayFromNow() {
        return departureTime == null || !departureTime.isBefore(LocalDateTime.now().plusDays(1));
    }

    @AssertTrue(message = ScheduleMessages.ARRIVAL_TIME_INVALID)
    public boolean isArrivalTimeValid() {
        return departureTime == null || arrivalTime == null || arrivalTime.isAfter(departureTime);
    }
}
