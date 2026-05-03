package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.dto.RouteActionDTO;
import vn.edu.iuh.fit.common.dto.RouteStopDTO;
import vn.edu.iuh.fit.common.response.Response;

public interface RouteStopService {
    Response findRouteStopsByRouteId(RouteActionDTO dto);

    Response createRouteStop(RouteStopDTO dto);

    Response updateRouteStop(RouteStopDTO dto);

    Response deleteRouteStop(RouteStopDTO dto);
}
