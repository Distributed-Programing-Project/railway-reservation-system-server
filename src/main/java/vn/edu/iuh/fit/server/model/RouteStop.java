package vn.edu.iuh.fit.server.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"stationStop", "route", "scheduleDetails"})
@Builder
@Entity
@Table(name = "route_stops")
public class RouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "route_stop_id", length = 36)
    private String id;

    @Column(name = "order_stop")
    private int orderStop ;

    @ManyToOne
    @JoinColumn(name = "station_stop_id")
    private Station stationStop;

    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route;

    @OneToMany(mappedBy = "routeStop")
    private List<ScheduleDetail> scheduleDetails;
}
