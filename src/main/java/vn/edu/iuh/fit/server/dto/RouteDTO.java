package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.server.constant.RouteStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteDTO implements Serializable {
    private String id;
    private String routeCode;
    private String departureStationId;
    private String destinationStationId;
    private RouteStatus status;
    private Double priceBasic;
}
