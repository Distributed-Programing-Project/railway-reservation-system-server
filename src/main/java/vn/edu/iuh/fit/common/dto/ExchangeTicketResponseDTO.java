package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeTicketResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String invoiceId;
    private double totalAmount;
    private int oldTicketCount;
    private int newTicketCount;
    private List<IssuedTicketDTO> newTickets;
}
