package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnTicketConfirmDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "ticketIds is required")
    private List<String> ticketIds;

    @Min(value = 0, message = "refundAmount must be >= 0")
    private double refundAmount;

    @NotEmpty(message = "employeeId is required")
    private String employeeId;
}

