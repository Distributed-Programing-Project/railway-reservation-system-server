package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.SeatType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDetailDTO implements Serializable {
  private String id;
  private double seatPrice;
  private String scheduleId;
  private String seatId;
  private String routeStopId;
  private String segmentDepartureStationId;
  private String segmentDepartureStationName;
  private String segmentDestinationStationId;
  private String segmentDestinationStationName;
  private Integer segmentDepartureOrder;
  private Integer segmentDestinationOrder;
  private int seatNumber;
  private SeatType seatType;
  private int carriageNumber;
  private boolean sold;
}
