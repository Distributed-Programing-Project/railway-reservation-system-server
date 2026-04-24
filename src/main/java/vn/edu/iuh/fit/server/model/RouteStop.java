package vn.edu.iuh.fit.server.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"stationStop", "route", "scheduleDetails"})
@Builder
@Entity
@Table(name = "route_stops")
@BatchSize(size = 25)
public class RouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "route_stop_id", length = 36)
    private String id;

    @Column(name = "order_stop")
    private int orderStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_stop_id")
    private Station stationStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @OneToMany(mappedBy = "routeStop")
    private List<ScheduleDetail> scheduleDetails;
}
