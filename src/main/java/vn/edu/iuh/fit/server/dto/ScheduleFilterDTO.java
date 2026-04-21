package vn.edu.iuh.fit.server.dto;

import jakarta.validation.constraints.AssertTrue;
import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.server.constant.StatusSchedule;
import vn.edu.iuh.fit.server.messages.ScheduleMessages;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleFilterDTO implements Serializable {
    private static final long serialVersionUID = 1L;

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
