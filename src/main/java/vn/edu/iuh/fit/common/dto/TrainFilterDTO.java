package vn.edu.iuh.fit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.TrainStatus;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainFilterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private TrainStatus statusFilter;
}
