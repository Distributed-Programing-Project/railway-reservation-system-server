package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.dto.RouteActionDTO;
import vn.edu.iuh.fit.common.dto.RouteDTO;
import vn.edu.iuh.fit.common.dto.RouteFilterDTO;
import vn.edu.iuh.fit.common.response.Response;

public interface RouteService {

    Response getAllRoutes();

    Response searchRoutes(RouteFilterDTO filter);

    Response findRouteById(String routeId);

    Response createRoute(RouteDTO routeDTO);

    Response updateRoute(RouteDTO routeDTO);

    Response deleteRoute(RouteActionDTO dto);

    Response promoteRoute(RouteActionDTO dto);

    Response disableRoute(RouteActionDTO dto);
}

