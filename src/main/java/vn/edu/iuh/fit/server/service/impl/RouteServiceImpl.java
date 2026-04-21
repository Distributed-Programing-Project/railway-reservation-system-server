package vn.edu.iuh.fit.server.service.impl;

import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.service.RouteService;

import java.util.List;

public class RouteServiceImpl implements RouteService {
    @Override
    public boolean createRoute(Route route) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean updateRoute(Route route) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Route findReverseRoute(String routeId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean hasSchedules(String routeId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean deleteRoute(String routeId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Route findRouteById(String routeId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Route> findAllRoutes() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Route> searchRoutes(String departureStationId, String destinationStationId, String status) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}

