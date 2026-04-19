package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.server.constant.StationStatus;

import java.util.List;

@Entity
@Table(name = "routes")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ToString.Exclude()
    @ManyToOne
    @JoinColumn(name = "departure_station_id" , nullable = false)
    private Station departureStation; // Điểm đi

    @ToString.Exclude()
    @ManyToOne
    @JoinColumn(name = "destination_station_id" , nullable = false)
    private Station destinationStation ;  // Điểm đến

    @Enumerated(EnumType.STRING)
    private StationStatus status;

    @Column(name="price_basic")
    private Double priceBasic;

    @ToString.Exclude()
    @OneToMany(mappedBy = "route")
    private List<RouteStop> routeStops ;

    @ToString.Exclude()
    @OneToMany(mappedBy = "route")
    private List<Schedule> schedules ;
}

