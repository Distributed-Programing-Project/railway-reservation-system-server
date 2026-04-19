package vn.edu.iuh.fit.client.controller;

import vn.edu.iuh.fit.server.model.RouteStop;

import java.util.List;

public interface RouteStopController {
    List<RouteStop> findAllRouteStops();

    List<RouteStop> findRouteStopsByRouteId(String routeId);

    RouteStop findRouteStopById(String routeStopId);

    boolean createRouteStop(RouteStop routeStop);

    boolean updateRouteStop(RouteStop routeStop);

    boolean deleteRouteStop(String routeStopId);
}

