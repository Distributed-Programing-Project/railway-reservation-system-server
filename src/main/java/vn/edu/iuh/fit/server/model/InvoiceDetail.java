package vn.edu.iuh.fit.server.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = { "invoice", "ticket" })
@Builder
@Entity
@Table(name = "invoice_details")
public class InvoiceDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "invoice_detail_id", length = 36)
  private String id;

  @ManyToOne
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false, unique = true)
  private Ticket ticket;

  @Column(name = "sub_total")
  private double subTotal;

  @Column(name = "discount")
  private double discount;

  @Column(name = "insurance_fee")
  private double insurance;

  @Column(name = "is_returned")
  private boolean isReturned;

  @Column(name = "refund_amount")
  private double refundAmount;
}
