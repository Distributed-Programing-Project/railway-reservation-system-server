package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.server.constant.TicketStatus;
import vn.edu.iuh.fit.server.constant.TicketType;

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
}
