package vn.edu.iuh.fit.server.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;

@Entity
@Table(name = "route_stops")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class RouteStop {
    @Id
    @GeneratedValue
    @UuidGenerator
    private String id ;

    @Column(name = "order_stop")
    private int orderStop ;

    @ToString.Exclude()
    @ManyToOne
    @JoinColumn(name = "station_stop_id")
    private Station stationStop ;

    @ToString.Exclude()
    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route ;

    @ToString.Exclude()
    @OneToMany(mappedBy = "routeStop")
    private List<ScheduleDetail> scheduleDetails ;
}
