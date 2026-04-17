package vn.edu.iuh.fit.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@ToString(exclude = { "schedule", "seat", "ticket" })
@Builder
@Entity
@Table(name = "schedule_details")
public class ScheduleDetail {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "schedule_detail_id", length = 36)
  private String id;

  @Column(name = "seat_price")
  private double seatPrice;

  @ManyToOne
  @JoinColumn(name = "schedule_id")
  private Schedule schedule;

  @ManyToOne
  @JoinColumn(name = "seat_id")
  private Seat seat;

  @OneToOne(mappedBy = "scheduleDetail")
  private Ticket ticket;
}
