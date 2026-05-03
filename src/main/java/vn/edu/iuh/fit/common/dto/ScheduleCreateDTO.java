package vn.edu.iuh.fit.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.message.ScheduleMessages;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = ScheduleMessages.EMPLOYEE_ID_REQUIRED)
    private String requestEmployeeId;

    @NotBlank(message = ScheduleMessages.TRAIN_ID_REQUIRED)

    private String trainId;

    @NotBlank(message = ScheduleMessages.ROUTE_ID_REQUIRED)
    private String routeId;

    @NotNull(message = ScheduleMessages.DEPARTURE_TIME_REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private LocalDateTime departureTime;

    @NotNull(message = ScheduleMessages.ARRIVAL_TIME_REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime arrivalTime;

    @AssertTrue(message = ScheduleMessages.DEPARTURE_TIME_MIN_ONE_DAY)
    public boolean isDepartureTimeAtLeastOneDayFromNow() {
        return departureTime == null || !departureTime.isBefore(LocalDateTime.now().plusDays(1));
    }

    @AssertTrue(message = ScheduleMessages.ARRIVAL_TIME_INVALID)
    public boolean isArrivalTimeValid() {
        return departureTime == null || arrivalTime == null || arrivalTime.isAfter(departureTime);
    }
}
