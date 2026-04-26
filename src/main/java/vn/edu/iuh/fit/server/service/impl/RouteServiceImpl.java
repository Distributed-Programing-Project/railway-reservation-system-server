package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.dto.RouteDTO;
import vn.edu.iuh.fit.common.message.RouteMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.repository.RouteRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.RouteRepositoryImpl;
import vn.edu.iuh.fit.server.service.RouteService;

import java.util.List;

public class RouteServiceImpl implements RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteServiceImpl.class);
    private final RouteRepository repository = new RouteRepositoryImpl();

    @Override
    public Response getAllRoutes() {
        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                List<Route> routes = repository.findAllRoutes(em);
                List<RouteDTO> dtos = routes.stream()
                        .map(r -> RouteDTO.builder()
                                .id(r.getId())
                                .routeCode(r.getRouteCode())
                                .departureStationId(r.getDepartureStation() != null ? r.getDepartureStation().getId() : null)
                                .departureStationName(r.getDepartureStation() != null ? r.getDepartureStation().getName() : null)
                                .destinationStationId(r.getDestinationStation() != null ? r.getDestinationStation().getId() : null)
                                .destinationStationName(r.getDestinationStation() != null ? r.getDestinationStation().getName() : null)
                                .status(r.getStatus())
                                .priceBasic(r.getPriceBasic())
                                .build())
                        .toList();
                return Response.success(RouteMessages.FIND_ALL_SUCCESS, dtos);
            });
        } catch (Exception e) {
            log.error("Failed to get all routes", e);
            return Response.error(RouteMessages.FIND_ALL_FAILED + e.getMessage());
        }
    }

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

