package vn.edu.iuh.fit.server.model;


import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.server.constant.TrainStatus;

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

  @Column(name = "train_code", unique = true, nullable = false, length = 10)
  private String trainCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 50)
  private TrainStatus status;

  @OneToMany(mappedBy = "train")
  private List<Carriage> carriages;
}
