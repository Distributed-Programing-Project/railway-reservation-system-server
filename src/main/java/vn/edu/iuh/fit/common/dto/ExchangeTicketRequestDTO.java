package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.message.TicketMessages;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeTicketRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = TicketMessages.OLD_TICKET_IDS_REQUIRED)
    private List<String> oldTicketIds;

    @NotEmpty(message = TicketMessages.NEW_SCHEDULE_DETAIL_IDS_REQUIRED)
    private List<String> newScheduleDetailIds;

    @Min(value = 0, message = TicketMessages.CASH_RECEIVED_NOT_NEGATIVE)
    private double cashReceived;

    // Hỗ trợ xuất VAT
    private String taxCode;
    private String companyName;
}
