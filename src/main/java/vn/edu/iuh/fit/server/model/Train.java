package vn.edu.iuh.fit.server.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "carriages")
@Builder
@Entity
@Table(name = "trains")
public class Train {

  @Id
  @Column(name = "train_id", length = 50)
  private String id;

  @Column(name = "status", columnDefinition = "NVARCHAR(50)")
  private String status;

  @OneToMany(mappedBy = "train")
  private List<Carriage> carriages;
}
