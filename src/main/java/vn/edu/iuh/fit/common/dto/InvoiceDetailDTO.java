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
public class InvoiceDetailDTO implements Serializable {
  private String id;
  private String invoiceId;
  private String ticketId;
  private Double subTotal;
  private double discount;
  private double insurance;
  private boolean isReturned;
  private double refundAmount;
}
