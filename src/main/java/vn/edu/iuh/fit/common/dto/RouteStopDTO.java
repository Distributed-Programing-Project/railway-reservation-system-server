package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStopDTO implements Serializable {
    private String id;
    private int orderStop;
    private String stationStopId;
    private String routeId;
}
