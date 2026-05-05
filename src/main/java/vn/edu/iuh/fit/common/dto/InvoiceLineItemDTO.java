package vn.edu.iuh.fit.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.TicketType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String invoiceId;
    private String ticketId;
    private String passengerName;
    private String passengerIdCard;
    private TicketType ticketType;
    private String trainName;
    private String carriageName;
    private String seatCode;
    private String departureStation;
    private String arrivalStation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime departureTime;

    private Double subTotal;
    private double discount;
    private double insurance;
    private double finalAmount;
    private boolean isReturned;
    private double refundAmount;
    private OriginalTicketInfoDTO originalTicketInfo;
}
