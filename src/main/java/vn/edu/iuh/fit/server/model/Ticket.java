package vn.edu.iuh.fit.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.BatchSize;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.common.constant.TicketType;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = { "customer", "scheduleDetail" })
@Builder
@Entity
@Table(name = "tickets")
@BatchSize(size = 25)
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "ticket_id", length = 36)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "schedule_detail_id")
  private ScheduleDetail scheduleDetail;

  @Enumerated(EnumType.STRING)
  @Column(name = "ticket_type")
  private TicketType type;

  @Column(name = "is_round_trip")
  private boolean roundTrip;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 50)
  private TicketStatus status;

  @Column(name = "qr_code", columnDefinition = "TEXT")
  private String qrCode;

  @Column(name = "original_ticket_id", length = 36)
  private String originalTicketId;

  @Column(name = "is_exchanged")
  private boolean exchanged;

  @Column(name = "passenger_name", columnDefinition = "NVARCHAR(255)")
  private String passengerName;

  @Column(name = "passenger_id_card", length = 20)
  private String passengerIdCard;
}