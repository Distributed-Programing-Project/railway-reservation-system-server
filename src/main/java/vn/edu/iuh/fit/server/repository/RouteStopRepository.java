package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.RouteStop;

import java.util.List;

public interface RouteStopRepository {
    List<RouteStop> findAllRouteStops(EntityManager em);

    List<RouteStop> findRouteStopsByRouteId(EntityManager em, String routeId);

    RouteStop findRouteStopById(EntityManager em, String routeStopId);

    boolean createRouteStop(EntityManager em, RouteStop routeStop);

    boolean updateRouteStop(EntityManager em, RouteStop routeStop);

    boolean deleteRouteStop(EntityManager em, String routeStopId);

    int deleteRouteStopsByRouteId(EntityManager em, String routeId);
}

