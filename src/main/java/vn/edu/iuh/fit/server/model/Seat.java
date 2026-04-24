package vn.edu.iuh.fit.server.model;

import java.util.List;

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
import vn.edu.iuh.fit.common.constant.SeatType;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"carriage", "scheduleDetails"})
@Builder
@Entity
@Table(name = "seats")
public class Seat {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "seat_id", length = 36)
  private String id;

  @Column(name = "sequence_number")
  private int number;

  @Column(name = "is_available")
  private boolean available;

  @Enumerated(EnumType.STRING)
  @Column(name = "seat_type")
  private SeatType type;

  @ManyToOne
  @JoinColumn(name = "carriage_id")
  private Carriage carriage;

  @OneToMany(mappedBy = "seat")
  private List<ScheduleDetail> scheduleDetails;
}