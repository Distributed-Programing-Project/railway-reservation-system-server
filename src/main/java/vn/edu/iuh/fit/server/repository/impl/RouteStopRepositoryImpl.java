package vn.edu.iuh.fit.server.repository.impl;

import vn.edu.iuh.fit.server.model.RouteStop;
import vn.edu.iuh.fit.server.repository.RouteStopRepository;

import java.util.List;

public class RouteStopRepositoryImpl extends AbstractGenericRepositoryImpl<RouteStop, String>
        implements RouteStopRepository {

    public RouteStopRepositoryImpl() {
        super(RouteStop.class);
    }

    @Override
    public List<RouteStop> findAllRouteStops() {
        return doWithEntityManager(em ->
                em.createQuery("SELECT rs FROM RouteStop rs", RouteStop.class).getResultList());
    }

    @Override
    public List<RouteStop> findRouteStopsByRouteId(String routeId) {
        return doWithEntityManager(em ->
                em.createQuery("SELECT rs FROM RouteStop rs WHERE rs.route.id = :id ORDER BY rs.stopOrder ASC", RouteStop.class)
                        .setParameter("id", routeId)
                        .getResultList());
    }

    @Override
    public RouteStop findRouteStopById(String routeStopId) {
        return doWithEntityManager(em -> em.find(RouteStop.class, routeStopId));
    }

    @Override
    public boolean createRouteStop(RouteStop routeStop) {
        return doInTransaction(em -> {
            em.persist(routeStop);
            return true;
        });
    }

    @Override
    public boolean updateRouteStop(RouteStop routeStop) {
        return doInTransaction(em -> {
            em.merge(routeStop);
            return true;
        });
    }

    @Override
    public boolean deleteRouteStop(String routeStopId) {
        return doInTransaction(em -> {
            RouteStop routeStop = em.find(RouteStop.class, routeStopId);
            if (routeStop != null) {
                em.remove(routeStop);
                return true;
            }
            return false;
        });
    }
}
