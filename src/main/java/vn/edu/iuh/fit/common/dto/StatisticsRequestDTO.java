package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.StatisticsPeriod;
import vn.edu.iuh.fit.common.message.StatisticsMessages;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticsRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = StatisticsMessages.PERIOD_TYPE_REQUIRED)
    private StatisticsPeriod periodType;

    @NotNull(message = StatisticsMessages.TARGET_DATE_REQUIRED)
    private LocalDate targetDate;

    @NotBlank(message = StatisticsMessages.EMPLOYEE_ID_REQUIRED)
    private String requestEmployeeId;

    private String employeeId;
}
