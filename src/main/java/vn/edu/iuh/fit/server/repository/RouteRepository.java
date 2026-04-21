package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Route;

import java.util.List;

public interface RouteRepository {
    boolean createRoute(EntityManager em, Route route);

    boolean updateRoute(EntityManager em, Route route);

    Route findReverseRoute(EntityManager em, String routeId);

    boolean hasSchedules(EntityManager em, String routeId);

    boolean deleteRoute(EntityManager em, String routeId);

    Route findRouteById(EntityManager em, String routeId);

    List<Route> findAllRoutes(EntityManager em);

    List<Route> searchRoutes(EntityManager em, String departureStationId, String destinationStationId, String status);
}

