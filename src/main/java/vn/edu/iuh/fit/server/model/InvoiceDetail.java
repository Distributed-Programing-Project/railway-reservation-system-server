package vn.edu.iuh.fit.server.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@ToString(exclude = { "invoice", "ticket" })
@Entity
@Table(name = "invoice_details")
@IdClass(InvoiceDetail.InvoiceDetailId.class)
public class InvoiceDetail {
  @Id
  @ManyToOne
  @JoinColumn(name = "invoice_id")
  private Invoice invoice;

  @Id
  @OneToOne
  @JoinColumn(name = "ticket_id")
  private Ticket ticket;

  @Column(name = "sub_total")
  private double subTotal;

  @Column(name = "discount")
  private double discount;

  @Column(name = "insurance_fee")
  private final double insurance = 2000.0;

  @Column(name = "is_returned")
  private boolean isReturned;

  @Column(name = "refund_amount")
  private double refundAmount;

  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  @Getter
  @EqualsAndHashCode
  @ToString
  public static class InvoiceDetailId implements Serializable {
    private String invoice;
    private String ticket;
  }
}
