package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.SeatType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerHistoryItemDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime purchaseTime;
  private String ticketId;

  private String trainCode;
  private String departureStationName;
  private String destinationStationName;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime departureTime;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime arrivalTime;

  private Integer carriageNumber;
  private SeatType seatType;
  private Integer seatNumber;

  private double ticketPrice;
}
