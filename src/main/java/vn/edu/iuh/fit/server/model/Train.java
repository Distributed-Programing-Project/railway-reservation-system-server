package vn.edu.iuh.fit.server.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.common.constant.TrainStatus;
import vn.edu.iuh.fit.server.util.id.annotation.GeneratedTrainId;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"carriages"})
@Builder
@Entity
@Table(name = "trains")
public class Train {

  @Id
  @GeneratedTrainId
  @Column(name = "train_id", length = 6)
  private String id;

  @Column(name = "train_code", unique = true, nullable = false, length = 10)
  private String trainCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 50)
  private TrainStatus status;

  @JsonIgnore
  @OneToMany(mappedBy = "train")
  private List<Carriage> carriages;
}
