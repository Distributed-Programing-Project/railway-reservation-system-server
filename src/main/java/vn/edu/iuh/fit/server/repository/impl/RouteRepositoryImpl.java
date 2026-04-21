package vn.edu.iuh.fit.server.repository.impl;

import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.repository.RouteRepository;

import java.util.List;

public class RouteRepositoryImpl extends AbstractGenericRepositoryImpl<Route, String>
        implements RouteRepository {

    public RouteRepositoryImpl() {
        super(Route.class);
    }

    @Override
    public boolean createRoute(Route route) {
        return doInTransaction(em -> {
            em.persist(route);
            return true;
        });
    }

    @Override
    public boolean updateRoute(Route route) {
        return doInTransaction(em -> {
            em.merge(route);
            return true;
        });
    }

    @Override
    public Route findReverseRoute(String routeId) {
        return doWithEntityManager(em -> {
            Route route = em.find(Route.class, routeId);
            if (route == null) return null;
            return em.createQuery("SELECT r FROM Route r WHERE r.departureStation = :dest AND r.destinationStation = :dep", Route.class)
                    .setParameter("dest", route.getDestinationStation())
                    .setParameter("dep", route.getDepartureStation())
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        });
    }

    @Override
    public boolean hasSchedules(String routeId) {
        return doWithEntityManager(em ->
                em.createQuery("SELECT COUNT(s) FROM Schedule s WHERE s.route.id = :id", Long.class)
                        .setParameter("id", routeId)
                        .getSingleResult() > 0);
    }

    @Override
    public boolean deleteRoute(String routeId) {
        return doInTransaction(em -> {
            Route route = em.find(Route.class, routeId);
            if (route != null) {
                em.remove(route);
                return true;
            }
            return false;
        });
    }

    @Override
    public Route findRouteById(String routeId) {
        return doWithEntityManager(em -> em.find(Route.class, routeId));
    }

    @Override
    public List<Route> findAllRoutes() {
        return doWithEntityManager(em ->
                em.createQuery("SELECT r FROM Route r", Route.class).getResultList());
    }

    @Override
    public List<Route> searchRoutes(String departureStationId, String destinationStationId, String status) {
        return doWithEntityManager(em -> {
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
        });
    }

    @Override
    public List<Route> getAllRoutes() {
        return findAllRoutes();
    }
}
