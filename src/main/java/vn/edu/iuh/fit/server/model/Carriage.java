package vn.edu.iuh.fit.server.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import vn.edu.iuh.fit.common.enums.CarriageType;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"train", "seats"})
@Builder
@Entity
@Table(name = "carriages")
public class Carriage {

  @Id
  @Column(name = "carriage_id", length = 50)
  private String id;

  @Column(name = "sequence_number")
  private int number;

  @Enumerated(EnumType.STRING)
  @Column(name = "carriage_type")
  private CarriageType type;

  @ManyToOne
  @JoinColumn(name = "train_id")
  private Train train;

  @OneToMany(mappedBy = "carriage")
  private List<Seat> seats;
}
