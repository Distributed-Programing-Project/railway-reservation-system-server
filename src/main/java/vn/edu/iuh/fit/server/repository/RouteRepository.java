package vn.edu.iuh.fit.server.repository;

import vn.edu.iuh.fit.server.model.Route;

import java.util.List;

public interface RouteRepository {
    boolean createRoute(Route route);

    boolean updateRoute(Route route);

    Route findReverseRoute(String routeId);

    boolean hasSchedules(String routeId);

    boolean deleteRoute(String routeId);

    Route findRouteById(String routeId);

    List<Route> findAllRoutes();

    List<Route> searchRoutes(String departureStationId, String destinationStationId, String status);

    List<Route> getAllRoutes();
}

