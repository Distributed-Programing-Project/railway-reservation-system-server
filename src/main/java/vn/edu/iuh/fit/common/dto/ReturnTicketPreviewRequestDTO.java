package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.util.List;

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
public class ReturnTicketPreviewRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = TicketMessages.TICKET_IDS_REQUIRED)
    private List<String> ticketIds;
}

