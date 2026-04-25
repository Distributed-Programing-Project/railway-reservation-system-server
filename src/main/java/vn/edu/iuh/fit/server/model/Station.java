package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"routeFrom", "routeTo", "routeStops"})
@Builder
@Entity
@Table(name = "stations")
@BatchSize(size = 25)
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "station_id", length = 36)
    private String id;

    @Column(name = "station_name", columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(name = "destination_km")
    private Float destinationKm;

    @OneToMany(mappedBy = "departureStation")
    private List<Route> routeFrom;

    @OneToMany(mappedBy = "destinationStation")
    private List<Route> routeTo;

    @OneToMany(mappedBy = "stationStop")
    private List<RouteStop> routeStops;
}
