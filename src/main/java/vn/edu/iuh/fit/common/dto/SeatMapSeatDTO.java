package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.SeatAvailabilityStatus;
import vn.edu.iuh.fit.common.constant.SeatType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatMapSeatDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String seatId;
  private int seatNumber;
  private SeatType seatType;
  private String scheduleDetailId;
  private double seatPrice;
  private SeatAvailabilityStatus seatStatus;
  private boolean heldByMe;
  private Long holdExpiresAtEpochMillis;
}
