package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDate;

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
public class ScheduleFilterDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = ScheduleMessages.EMPLOYEE_ID_REQUIRED)
    private String requestEmployeeId;
    private String departureStationId;
    private String destinationStationId;
    private String trainId;
    private StatusSchedule status;
    private LocalDate fromDate;
    private LocalDate toDate;
    
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 20;

    @AssertTrue(message = ScheduleMessages.DATE_RANGE_INVALID)
    public boolean isDateRangeValid() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }
}
