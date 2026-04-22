package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnTicketPreviewDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private double totalTicketPrice;
    private double refundFee;
    private double refundAmount;
}

