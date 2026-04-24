package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"seat", "schedule", "routeStop"})
@Builder
@Entity
@Table(name = "schedule_details")
@BatchSize(size = 25)
public class ScheduleDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "schedule_detail_id", length = 36)
    private String id;

    @Column(name = "price_seat")
    private BigDecimal priceSeat;

    @ManyToOne(fetch = FetchType.LAZY)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_stop_id")
    private RouteStop routeStop;


    @Version
    private int version;
}