package vn.edu.iuh.fit.server.dto;

import jakarta.validation.constraints.AssertTrue;
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

    @NotBlank(message = "Mã tàu không được để trống")
    private String trainId;

    @NotBlank(message = "Mã tuyến không được để trống")
    private String routeId;

    @NotNull(message = "Thời gian khởi hành không được để trống")
    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    @AssertTrue(message = "Ngày khởi hành phải cách ít nhất 1 ngày so với hôm nay")
    public boolean isDepartureTimeAtLeastOneDayFromNow() {
        return departureTime == null || !departureTime.isBefore(LocalDateTime.now().plusDays(1));
    }

    @AssertTrue(message = "Ngày giờ đến dự kiến không được nhỏ hơn giờ khởi hành")
    public boolean isArrivalTimeValid() {
        return departureTime == null || arrivalTime == null || !arrivalTime.isBefore(departureTime);
    }
}
