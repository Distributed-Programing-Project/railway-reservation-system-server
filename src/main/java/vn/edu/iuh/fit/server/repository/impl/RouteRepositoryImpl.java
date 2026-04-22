package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.repository.RouteRepository;

import java.util.List;

public class RouteRepositoryImpl extends AbstractGenericRepositoryImpl<Route, String>
        implements RouteRepository {

    public RouteRepositoryImpl() {
        super(Route.class);
    }

    @Override
    public boolean createRoute(EntityManager em, Route route) {
        em.persist(route);
        return true;
    }

    @Override
    public boolean updateRoute(EntityManager em, Route route) {
        em.merge(route);
        return true;
    }

    @Override
    public Route findReverseRoute(EntityManager em, String routeId) {
        Route route = em.find(Route.class, routeId);
        if (route == null) return null;
        return em.createQuery("SELECT r FROM Route r WHERE r.departureStation = :dest AND r.destinationStation = :dep", Route.class)
                .setParameter("dest", route.getDestinationStation())
                .setParameter("dep", route.getDepartureStation())
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean hasSchedules(EntityManager em, String routeId) {
        return em.createQuery("SELECT COUNT(s) FROM Schedule s WHERE s.route.id = :id", Long.class)
                .setParameter("id", routeId)
                .getSingleResult() > 0;
    }

    @Override
    public boolean deleteRoute(EntityManager em, String routeId) {
        Route route = em.find(Route.class, routeId);
        if (route != null) {
            em.remove(route);
            return true;
        }
        return false;
    }

    @Override
    public Route findRouteById(EntityManager em, String routeId) {
        return em.find(Route.class, routeId);
    }

    @Override
    public List<Route> findAllRoutes(EntityManager em) {
        return em.createQuery("SELECT r FROM Route r", Route.class).getResultList();
    }

    @Override
    public List<Route> searchRoutes(EntityManager em, String departureStationId, String destinationStationId, String status) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Route r WHERE 1=1 ");
        if (departureStationId != null && !departureStationId.isEmpty()) {
            jpql.append("AND r.departureStation.id = :depId ");
        }
        if (destinationStationId != null && !destinationStationId.isEmpty()) {
            jpql.append("AND r.destinationStation.id = :destId ");
        }
        if (status != null && !status.isEmpty()) {
            jpql.append("AND r.status = :status ");
        }

        var query = em.createQuery(jpql.toString(), Route.class);
        if (departureStationId != null && !departureStationId.isEmpty()) {
            query.setParameter("depId", departureStationId);
        }
        if (destinationStationId != null && !destinationStationId.isEmpty()) {
            query.setParameter("destId", destinationStationId);
        }
        if (status != null && !status.isEmpty()) {
            query.setParameter("status", status);
        }

        return query.getResultList();
    }

}
