package vn.edu.iuh.fit.server.dto;

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
import vn.edu.iuh.fit.server.constant.StatusSchedule;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDTO implements Serializable {
    @NotBlank(message = "Mã lịch trình không được để trống")
    private String id;

    @NotNull(message = "Thời gian khởi hành không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime arrivalTime;

    private StatusSchedule status;

    @NotBlank(message = "Mã tàu không được để trống")
    private String trainId;

    private String trainName;

    @NotBlank(message = "Mã tuyến không được để trống")
    private String routeId;

    private String routeCode;
    private String departureStationName;
    private String destinationStationName;

    @AssertTrue(message = "Ngày khởi hành phải cách ít nhất 1 ngày so với hôm nay")
    public boolean isDepartureTimeAtLeastOneDayFromNow() {
        return departureTime == null || !departureTime.isBefore(LocalDateTime.now().plusDays(1));
    }

    @AssertTrue(message = "Ngày giờ đến dự kiến không được nhỏ hơn giờ khởi hành")
    public boolean isArrivalTimeValid() {
        return departureTime == null || arrivalTime == null || !arrivalTime.isBefore(departureTime);
    }
}
