package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.RouteStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteFilterDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String requestEmployeeId;
    private String departureStationId;
    private String destinationStationId;
    private RouteStatus status;
}
