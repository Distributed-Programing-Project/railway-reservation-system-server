package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;

@Table(name = "stations")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id ;

    private String name ;

    @Column(name = "destination_km")
    private Float destinationKm ;

    @ToString.Exclude
    @OneToMany(mappedBy = "departureStation")
    private List<Route> routeFrom ;

    @ToString.Exclude
    @OneToMany(mappedBy = "destinationStation")
    private List<Route> routeTo ;

    @ToString.Exclude
    @OneToMany(mappedBy = "stationStop")
    private List<RouteStop> routeStops ;
}
