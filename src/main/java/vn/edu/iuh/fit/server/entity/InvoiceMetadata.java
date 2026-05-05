package vn.edu.iuh.fit.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import vn.edu.iuh.fit.common.constant.PaymentMethod;
import vn.edu.iuh.fit.server.model.Invoice;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = "invoice")
@Builder
@Entity
@Table(name = "invoice_metadata")
public class InvoiceMetadata {

  @Id
  @Column(name = "invoice_id", length = 36)
  private String invoiceId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id")
  private Invoice invoice;

  @Column(name = "vat_address", columnDefinition = "NVARCHAR(255)")
  private String vatAddress;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", length = 20)
  private PaymentMethod paymentMethod;

  @Column(name = "payment_order_id", length = 36)
  private String paymentOrderId;

  @Column(name = "payment_reference_code", length = 20)
  private String paymentReferenceCode;
}
