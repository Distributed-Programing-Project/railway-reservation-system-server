package vn.edu.iuh.fit.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.server.constant.StatisticsPeriod;

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

    @NotNull(message = "Loại kỳ thống kê là bắt buộc")
    private StatisticsPeriod periodType;

    @NotNull(message = "Ngày đại diện kỳ là bắt buộc")
    private LocalDate targetDate;

    @NotBlank(message = "ID nhân viên yêu cầu là bắt buộc")
    private String requestEmployeeId;

    private String employeeId;
}