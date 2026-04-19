package vn.edu.iuh.fit.server.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString(exclude = "carriages")
@Builder
@Entity
@Table(name = "trains")
public class Train {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "train_id", length = 36)
  private String id;

  @Column(name = "status", columnDefinition = "NVARCHAR(50)")
  private String status;

  @OneToMany(mappedBy = "train")
  private List<Carriage> carriages;
}
