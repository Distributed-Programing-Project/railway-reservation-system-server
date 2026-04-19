package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDetailDTO implements Serializable {
  private int id;
  private double seatPrice;
  private String scheduleId;
  private String seatId;
}
