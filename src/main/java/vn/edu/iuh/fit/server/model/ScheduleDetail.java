package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@Builder
@Entity
@Table(name =  "schedule_details")
public class ScheduleDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "schedule_detail_id", length = 36)
    private String id;

    @Column(name = "price_seat")
    private BigDecimal priceSeat;

    @ToString.Exclude
    @ManyToOne
    private Seat seat;

    @ToString.Exclude
    @ManyToOne
    private Schedule schedule;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "route_stop_id")
    private RouteStop routeStop;
}