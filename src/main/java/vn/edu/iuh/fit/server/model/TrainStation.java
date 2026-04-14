package vn.edu.iuh.fit.server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
    private int id;
    private String position;
    private int kmMarker; // Lấy mốc Hà Nội là chuẩn là 0km

    @OneToMany(mappedBy = "trainStation")
    private List<Route> routeList;
}
