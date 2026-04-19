package vn.edu.iuh.fit.client.controller.impl;

import vn.edu.iuh.fit.client.controller.RouteStopController;
import vn.edu.iuh.fit.server.model.RouteStop;

import java.util.List;

public class RouteStopControllerImpl implements RouteStopController {
    @Override
    public List<RouteStop> findAllRouteStops() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<RouteStop> findRouteStopsByRouteId(String routeId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public RouteStop findRouteStopById(String routeStopId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean createRouteStop(RouteStop routeStop) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean updateRouteStop(RouteStop routeStop) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean deleteRouteStop(String routeStopId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

