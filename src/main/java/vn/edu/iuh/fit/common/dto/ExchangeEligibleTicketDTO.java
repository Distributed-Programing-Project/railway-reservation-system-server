package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.SeatType;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.common.constant.TicketType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeEligibleTicketDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String ticketId;
  private TicketStatus status;

  private String scheduleId;
  private LocalDateTime departureTime;
  private String trainCode;
  private String departureStation;
  private String destinationStation;

  private String scheduleDetailId;
  private String seatId;
  private SeatType seatType;
  private String carriageName;
  private String seatNumber;
  private double ticketPrice;
  private TicketType ticketType;

  private String passengerName;
  private String passengerIdCard;

  private boolean eligible;
  private String ineligibleReason;
}

