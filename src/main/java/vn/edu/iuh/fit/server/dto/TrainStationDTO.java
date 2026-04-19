package vn.edu.iuh.fit.server.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainStationDTO {
    private int id;
    private String position;
    private int kmMarker; // Lấy mốc Hà Nội là chuẩn là 0km
}
