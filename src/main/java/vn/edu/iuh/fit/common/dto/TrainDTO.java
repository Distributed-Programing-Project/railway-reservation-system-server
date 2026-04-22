package vn.edu.iuh.fit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.TrainStatus;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String trainCode;
    private TrainStatus status;
    private int totalCarriages;
    private int totalSeats;
    private List<CarriageDTO> carriages;
}
