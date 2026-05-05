package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeTicketPreviewDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private double totalOldPrice;
  private double totalNewPrice;
  private double exchangeFeePerTicket;
  private double exchangeFeeTotal;
  private double priceDifference;
  private double totalAmount;
}

