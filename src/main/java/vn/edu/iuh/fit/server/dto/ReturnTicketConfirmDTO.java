package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import vn.edu.iuh.fit.server.messages.TicketMessages;
import vn.edu.iuh.fit.server.messages.EmployeeMessages;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnTicketConfirmDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = TicketMessages.TICKET_IDS_REQUIRED)
    private List<String> ticketIds;

    @Min(value = 0, message = TicketMessages.REFUND_AMOUNT_INVALID)
    private double refundAmount;

    @NotEmpty(message = EmployeeMessages.EMPLOYEE_ID_REQUIRED)
    private String employeeId;
}

