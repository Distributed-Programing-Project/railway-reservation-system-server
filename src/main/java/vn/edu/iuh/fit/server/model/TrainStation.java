package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "train_stations")
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TrainStation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private int id;
    private String position;
    private int kmMarker; // Lấy mốc Hà Nội là chuẩn là 0km

    @OneToMany(mappedBy = "trainStation")
    private List<Route> routeList;
}
