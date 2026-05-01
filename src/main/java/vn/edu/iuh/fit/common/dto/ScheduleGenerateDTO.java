package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.message.ScheduleMessages;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleGenerateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = ScheduleMessages.EMPLOYEE_ID_REQUIRED)
    private String requestEmployeeId;

    @NotBlank(message = ScheduleMessages.TRAIN_ID_REQUIRED)
    private String trainId;

    @NotBlank(message = ScheduleMessages.ROUTE_ID_REQUIRED)
    private String routeId;

    @NotNull(message = ScheduleMessages.START_DATE_REQUIRED)
    private LocalDate startDate;

    @NotNull(message = ScheduleMessages.DEPARTURE_TIME_REQUIRED)
    private LocalTime departureTime;

    private int days;

    private boolean roundTrip;

    @AssertTrue(message = ScheduleMessages.GENERATE_DAYS_INVALID)
    public boolean isDaysValid() {
        return days == 7 || days == 30;
    }

    @AssertTrue(message = ScheduleMessages.DEPARTURE_TIME_MIN_ONE_DAY)
    public boolean isFirstDepartureAtLeastOneDayFromNow() {
        if (startDate == null || departureTime == null) {
            return true;
        }
        return !LocalDateTime.of(startDate, departureTime).isBefore(LocalDateTime.now().plusDays(1));
    }
}
