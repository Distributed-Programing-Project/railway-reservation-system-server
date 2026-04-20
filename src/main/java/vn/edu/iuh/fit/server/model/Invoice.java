package vn.edu.iuh.fit.server.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import vn.edu.iuh.fit.server.constant.InvoiceType;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = { "customer", "employee", "details" })
@Builder
@Entity
@Table(name = "invoices")
public class Invoice {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "invoice_id", length = 36)
  private String id;

  @Column(name = "issue_date")
  private LocalDateTime issueDate;

  @Column(name = "total_amount")
  private double totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "invoice_type")
  private InvoiceType type;

  @ManyToOne
  @JoinColumn(name = "customer_id")
  private Customer customer;

  @ManyToOne
  @JoinColumn(name = "employee_id")
  private Employee employee;

  @Column(name = "tax_code")
  private String taxCode;

  @Column(name = "company_name", columnDefinition = "NVARCHAR(255)")
  private String companyName;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
  private List<InvoiceDetail> details;
}
