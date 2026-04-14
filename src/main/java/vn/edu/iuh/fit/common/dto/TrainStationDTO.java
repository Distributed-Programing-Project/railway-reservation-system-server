package vn.edu.iuh.fit.common.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import vn.edu.iuh.fit.server.model.Route;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainStationDTO {
    private int id;
    private String position;
    private int kmMarker; // Lấy mốc Hà Nội là chuẩn là 0km
}
