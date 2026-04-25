package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

import vn.edu.iuh.fit.common.constant.RouteStatus;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"departureStation", "destinationStation", "routeStops", "schedules"})
@Builder
@Entity
@Table(name = "routes")
@BatchSize(size = 25)
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "route_id", length = 36)
    private String id;

    @Column(name = "route_code", length = 20)
    private String routeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_station_id", nullable = false)
    private Station departureStation; // Điểm đi

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_station_id", nullable = false)
    private Station destinationStation;  // Điểm đến

    @Enumerated(EnumType.STRING)
    private RouteStatus status;

    @Column(name="price_basic")
    private Double priceBasic;

    @OneToMany(mappedBy = "route")
    private List<RouteStop> routeStops;

    @OneToMany(mappedBy = "route")
    private List<Schedule> schedules;
}
