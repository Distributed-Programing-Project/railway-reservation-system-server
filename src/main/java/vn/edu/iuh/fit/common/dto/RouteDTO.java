package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.RouteStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String requestEmployeeId;
    private String id;
    private String routeCode;
    private String departureStationId;
    private String departureStationName;
    private String destinationStationId;
    private String destinationStationName;
    private RouteStatus status;
    private Double priceBasic;
    private List<RouteStopDTO> routeStops;
}
