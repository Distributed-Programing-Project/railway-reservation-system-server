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
import vn.edu.iuh.fit.common.constant.TicketType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuedTicketDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String ticketId;
  private String passengerName;
  private String passengerDocument;

  private String scheduleId;
  private String trainCode;
  private String departureStation;
  private String destinationStation;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime departureTime;

  private String carriageName;
  private String seatNumber;
  private SeatType seatType;

  private TicketType ticketType;
  private double price;
  private String qrCode;

  private boolean childUnder6;
  private String accompanyAdultTicketId;
}

