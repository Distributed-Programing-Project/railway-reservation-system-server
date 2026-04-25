package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.common.constant.TicketType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDTO implements Serializable {
  private String id;
  private String customerId;
  private String scheduleDetailId;
  private TicketType type;
  private boolean roundTrip;
  private TicketStatus status;
  private String qrCode;
  private String originalTicketId;
  private boolean exchanged;
  private String passengerName;
  private String passengerIdCard;
}
