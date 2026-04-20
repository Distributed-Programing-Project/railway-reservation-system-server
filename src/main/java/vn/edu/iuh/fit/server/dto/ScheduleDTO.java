package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime arrivalTime;

    private StatusSchedule status;
    private String trainId;
    private String trainName;
    private String routeId;
    private String routeCode;
    private String departureStationName;
    private String destinationStationName;
}
