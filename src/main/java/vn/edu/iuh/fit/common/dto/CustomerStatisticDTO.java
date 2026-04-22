package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerStatisticDTO implements Serializable {
    private String customerId;
    private String customerName;
    private String phoneNumber;
    private int totalTicketsPurchased;
    private BigDecimal totalAmount;
}
