package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = TicketMessages.EMPLOYEE_ID_REQUIRED)
    private String employeeId;

    @NotBlank(message = TicketMessages.INVALID_SESSION)
    private String clientSessionId;

    @Size(max = 20, message = TicketMessages.TAX_CODE_TOO_LONG)
    private String taxCode;

    @Size(max = 200, message = TicketMessages.COMPANY_NAME_TOO_LONG)
    private String companyName;
}
