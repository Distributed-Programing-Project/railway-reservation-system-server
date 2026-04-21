package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.RouteStop;
import vn.edu.iuh.fit.server.repository.RouteStopRepository;

import java.util.List;

public class RouteStopRepositoryImpl extends AbstractGenericRepositoryImpl<RouteStop, String>
        implements RouteStopRepository {

    public RouteStopRepositoryImpl() {
        super(RouteStop.class);
    }

    @Override
    public List<RouteStop> findAllRouteStops(EntityManager em) {
        return em.createQuery("SELECT rs FROM RouteStop rs", RouteStop.class).getResultList();
    }

    @Override
    public List<RouteStop> findRouteStopsByRouteId(EntityManager em, String routeId) {
        return em.createQuery("SELECT rs FROM RouteStop rs WHERE rs.route.id = :id ORDER BY rs.stopOrder ASC", RouteStop.class)
                .setParameter("id", routeId)
                .getResultList();
    }

    @Override
    public RouteStop findRouteStopById(EntityManager em, String routeStopId) {
        return em.find(RouteStop.class, routeStopId);
    }

    @Override
    public boolean createRouteStop(EntityManager em, RouteStop routeStop) {
        em.persist(routeStop);
        return true;
    }

    @Override
    public boolean updateRouteStop(EntityManager em, RouteStop routeStop) {
        em.merge(routeStop);
        return true;
    }

    @Override
    public boolean deleteRouteStop(EntityManager em, String routeStopId) {
        RouteStop routeStop = em.find(RouteStop.class, routeStopId);
        if (routeStop != null) {
            em.remove(routeStop);
            return true;
        }
        return false;
    }
}
