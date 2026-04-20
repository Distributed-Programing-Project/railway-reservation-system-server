package vn.edu.iuh.fit.server.dto;

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

    private String trainId;
    private String routeId;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
}
