package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

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
public class ReturnTicketTicketDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String customerId;
    private String scheduleDetailId;
    private String scheduleId;
    private LocalDateTime departureTime;
    private double ticketPrice;
    private TicketType type;
    private boolean roundTrip;
    private TicketStatus status;
    private String originalTicketId;
}

