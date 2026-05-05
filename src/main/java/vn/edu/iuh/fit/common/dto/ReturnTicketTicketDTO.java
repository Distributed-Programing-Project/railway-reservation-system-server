package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ReturnTicketTicketDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String customerId;
    private String scheduleDetailId;
    private String scheduleId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime departureTime;

    private String trainCode;
    private String departureStation;
    private String destinationStation;

    private String carriageName;
    private String seatNumber;

    private double ticketPrice;
    private TicketType type;
    private boolean roundTrip;
    private TicketStatus status;
    private String originalTicketId;
    private String passengerName;
    private String passengerIdCard;
}

