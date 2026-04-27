package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.StatusSchedule;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleSaleCardDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String scheduleId;
  private String trainId;
  private String trainCode;
  private String routeId;
  private String departureStationName;
  private String destinationStationName;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime departureTime;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime arrivalTime;

  private int availableSeats;
  private int totalSeats;
  private StatusSchedule status;
}

