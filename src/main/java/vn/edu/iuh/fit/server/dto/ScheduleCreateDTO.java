package vn.edu.iuh.fit.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "ID nhân viên không được để trống")
    private String requestEmployeeId;

    @NotBlank(message = "Mã tàu không được để trống")
    private String trainId;

    @NotBlank(message = "Mã tuyến không được để trống")
    private String routeId;

    @NotNull(message = "Thời gian khởi hành không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime departureTime;

    @NotNull(message = "Ngày giờ đến dự kiến không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime arrivalTime;
}
