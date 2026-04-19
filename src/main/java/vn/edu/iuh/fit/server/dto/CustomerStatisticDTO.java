package vn.edu.iuh.fit.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

public class CustomerStatisticDTO {
    private String customerId;
    private String customerName;
    private String phoneNumber;
    private int totalTicketsPurchased;
    private BigDecimal totalAmount;
}
