package vn.edu.iuh.fit.server.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@ToString(exclude = "tickets")
@Builder
@Entity
@Table(name = "customers")
public class Customer {
  @Id
  @Column(name = "customer_id", length = 50)
  private String id;

  @Column(name = "full_name", nullable = false, columnDefinition = "NVARCHAR(255)")
  private String name;

  @Column(name = "id_card", length = 20)
  private String idCard;

  @Column(name = "passport", length = 20)
  private String passport;

  @OneToMany(mappedBy = "customer")
  private List<Ticket> tickets;
}
