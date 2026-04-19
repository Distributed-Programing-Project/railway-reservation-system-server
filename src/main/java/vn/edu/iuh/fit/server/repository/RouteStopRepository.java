package vn.edu.iuh.fit.server.repository;

import vn.edu.iuh.fit.server.model.RouteStop;

import java.util.List;

public interface RouteStopRepository {
    List<RouteStop> findAllRouteStops();

    List<RouteStop> findRouteStopsByRouteId(String routeId);

    RouteStop findRouteStopById(String routeStopId);

    boolean createRouteStop(RouteStop routeStop);

    boolean updateRouteStop(RouteStop routeStop);

    boolean deleteRouteStop(String routeStopId);
}

