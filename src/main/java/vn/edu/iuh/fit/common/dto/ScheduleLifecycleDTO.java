package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.message.ScheduleMessages;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleLifecycleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = ScheduleMessages.EMPLOYEE_ID_REQUIRED)
    private String requestEmployeeId;

    @NotBlank(message = ScheduleMessages.SCHEDULE_ID_REQUIRED)
    private String scheduleId;

    @NotBlank(message = ScheduleMessages.ACTION_REQUIRED)
    private String action;
}
