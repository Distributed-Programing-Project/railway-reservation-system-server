package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.constant.RouteStatus;
import vn.edu.iuh.fit.common.dto.RouteActionDTO;
import vn.edu.iuh.fit.common.dto.RouteDTO;
import vn.edu.iuh.fit.common.dto.RouteFilterDTO;
import vn.edu.iuh.fit.common.dto.RouteStopDTO;
import vn.edu.iuh.fit.common.message.RouteMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.RouteStop;
import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.RouteRepository;
import vn.edu.iuh.fit.server.repository.RouteStopRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.RouteRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.RouteStopRepositoryImpl;
import vn.edu.iuh.fit.server.service.RouteService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RouteServiceImpl implements RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteServiceImpl.class);

    private final RouteRepository repository = new RouteRepositoryImpl();
    private final RouteStopRepository routeStopRepository = new RouteStopRepositoryImpl();
    private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();

    @Override
    public Response getAllRoutes() {
        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                List<RouteDTO> dtos = repository.findAllRoutes(em).stream()
                        .map(this::toDto)
                        .toList();
                return Response.success(RouteMessages.FIND_ALL_SUCCESS, dtos);
            });
        } catch (Exception e) {
            log.error("Failed to get all routes", e);
            return Response.error(RouteMessages.FIND_ALL_FAILED + e.getMessage());
        }
    }

    @Override
    public Response searchRoutes(RouteFilterDTO filter) {
        if (filter == null) {
            return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        }
        Response authError = requireActiveManager(filter.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                List<RouteDTO> dtos = repository.findAllRoutes(em).stream()
                        .filter(route -> filter.getStatus() == null || route.getStatus() == filter.getStatus())
                        .filter(route -> containsStationFilter(route, filter.getDepartureStationId(),
                                filter.getDestinationStationId()))
                        .map(this::toDto)
                        .toList();
                return Response.success(dtos.isEmpty() ? RouteMessages.FILTER_EMPTY : RouteMessages.FILTER_SUCCESS, dtos);
            });
        } catch (Exception e) {
            log.error("Failed to search routes", e);
            return Response.error(RouteMessages.FILTER_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response findRouteById(String routeId) {
        if (isBlank(routeId)) {
            return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        }
        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                Route route = repository.findRouteById(em, routeId);
                if (route == null) {
                    return Response.error(RouteMessages.notFound(routeId));
                }
                return Response.success(RouteMessages.FIND_BY_ID_SUCCESS, toDto(route));
            });
        } catch (Exception e) {
            log.error("Failed to find route by id: {}", routeId, e);
            return Response.error(RouteMessages.FIND_BY_ID_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response createRoute(RouteDTO routeDTO) {
        Response baseError = validateBase(routeDTO, false);
        if (baseError != null) return baseError;
        Response authError = requireActiveManager(routeDTO.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Station departure = em.find(Station.class, routeDTO.getDepartureStationId());
                if (departure == null) return Response.error(RouteMessages.stationNotFound(routeDTO.getDepartureStationId()));
                Station destination = em.find(Station.class, routeDTO.getDestinationStationId());
                if (destination == null) return Response.error(RouteMessages.stationNotFound(routeDTO.getDestinationStationId()));

                if (!repository.searchRoutes(em, routeDTO.getDepartureStationId(),
                        routeDTO.getDestinationStationId(), null).isEmpty()) {
                    return Response.error(RouteMessages.DUPLICATE_ROUTE);
                }

                StopValidation stopValidation = validateStops(
                        em, routeDTO.getRouteStops(), routeDTO.getDepartureStationId(),
                        routeDTO.getDestinationStationId());
                if (stopValidation.error() != null) return stopValidation.error();

                Route route = Route.builder()
                        .routeCode(routeDTO.getRouteCode().trim())
                        .departureStation(departure)
                        .destinationStation(destination)
                        .priceBasic(routeDTO.getPriceBasic())
                        .status(RouteStatus.DRAFT)
                        .build();
                repository.createRoute(em, route);
                persistStops(em, route, stopValidation.stops());
                log.info("Route created: id={}", route.getId());
                return Response.success(RouteMessages.CREATE_SUCCESS, route.getId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to create route", e);
            return Response.error(RouteMessages.CREATE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response updateRoute(RouteDTO routeDTO) {
        Response baseError = validateBase(routeDTO, true);
        if (baseError != null) return baseError;
        Response authError = requireActiveManager(routeDTO.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Route existing = repository.findRouteById(em, routeDTO.getId());
                if (existing == null) return Response.error(RouteMessages.notFound(routeDTO.getId()));
                if (repository.hasSchedules(em, routeDTO.getId())) {
                    return Response.error(RouteMessages.HAS_SCHEDULES_UPDATE);
                }

                Station departure = em.find(Station.class, routeDTO.getDepartureStationId());
                if (departure == null) return Response.error(RouteMessages.stationNotFound(routeDTO.getDepartureStationId()));
                Station destination = em.find(Station.class, routeDTO.getDestinationStationId());
                if (destination == null) return Response.error(RouteMessages.stationNotFound(routeDTO.getDestinationStationId()));

                boolean duplicated = repository.searchRoutes(em, routeDTO.getDepartureStationId(),
                                routeDTO.getDestinationStationId(), null)
                        .stream()
                        .anyMatch(route -> !route.getId().equals(routeDTO.getId()));
                if (duplicated) return Response.error(RouteMessages.DUPLICATE_ROUTE_OTHER);

                StopValidation stopValidation = validateStops(
                        em, routeDTO.getRouteStops(), routeDTO.getDepartureStationId(),
                        routeDTO.getDestinationStationId());
                if (stopValidation.error() != null) return stopValidation.error();

                existing.setRouteCode(routeDTO.getRouteCode().trim());
                existing.setDepartureStation(departure);
                existing.setDestinationStation(destination);
                existing.setPriceBasic(routeDTO.getPriceBasic());
                repository.updateRoute(em, existing);
                routeStopRepository.deleteRouteStopsByRouteId(em, existing.getId());
                persistStops(em, existing, stopValidation.stops());
                log.info("Route updated: id={}", existing.getId());
                return Response.success(RouteMessages.UPDATE_SUCCESS, existing.getId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to update route: id={}", routeDTO.getId(), e);
            return Response.error(RouteMessages.UPDATE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response deleteRoute(RouteActionDTO dto) {
        Response actionError = validateAction(dto);
        if (actionError != null) return actionError;
        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Route route = repository.findRouteById(em, dto.getRouteId());
                if (route == null) return Response.error(RouteMessages.notFound(dto.getRouteId()));
                if (route.getStatus() != RouteStatus.DRAFT) return Response.error(RouteMessages.DRAFT_DELETE_ONLY);
                if (repository.hasSchedules(em, dto.getRouteId())) return Response.error(RouteMessages.HAS_SCHEDULES_DELETE);
                repository.deleteRoute(em, dto.getRouteId());
                log.info("Route deleted: id={}", dto.getRouteId());
                return Response.success(RouteMessages.DELETE_SUCCESS, dto.getRouteId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to delete route: id={}", dto.getRouteId(), e);
            return Response.error(RouteMessages.DELETE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response promoteRoute(RouteActionDTO dto) {
        Response actionError = validateAction(dto);
        if (actionError != null) return actionError;
        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Route route = repository.findRouteById(em, dto.getRouteId());
                if (route == null) return Response.error(RouteMessages.notFound(dto.getRouteId()));
                if (route.getStatus() != RouteStatus.DRAFT) return Response.error(RouteMessages.DRAFT_PROMOTE_ONLY);
                route.setStatus(RouteStatus.ACTIVE);
                repository.updateRoute(em, route);
                log.info("Route promoted: id={}", dto.getRouteId());
                return Response.success(RouteMessages.PROMOTE_SUCCESS, dto.getRouteId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to promote route: id={}", dto.getRouteId(), e);
            return Response.error(RouteMessages.PROMOTE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response disableRoute(RouteActionDTO dto) {
        Response actionError = validateAction(dto);
        if (actionError != null) return actionError;
        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Route route = repository.findRouteById(em, dto.getRouteId());
                if (route == null) return Response.error(RouteMessages.notFound(dto.getRouteId()));
                if (route.getStatus() != RouteStatus.ACTIVE) return Response.error(RouteMessages.ACTIVE_DISABLE_ONLY);
                if (repository.hasSchedules(em, dto.getRouteId())) return Response.error(RouteMessages.HAS_SCHEDULES_DISABLE);
                route.setStatus(RouteStatus.PAUSED);
                repository.updateRoute(em, route);
                log.info("Route disabled: id={}", dto.getRouteId());
                return Response.success(RouteMessages.DISABLE_SUCCESS, dto.getRouteId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to disable route: id={}", dto.getRouteId(), e);
            return Response.error(RouteMessages.DISABLE_FAILED_PREFIX + e.getMessage());
        }
    }

    private Response requireActiveManager(String employeeId) {
        if (isBlank(employeeId)) {
            return Response.error(RouteMessages.EMPLOYEE_ID_REQUIRED);
        }
        return AbstractGenericRepositoryImpl.readOnly(em -> {
            Employee requester = employeeRepository.findEmployeeById(em, employeeId);
            if (requester == null) return Response.error(RouteMessages.employeeNotFoundById(employeeId));
            if (requester.getEmployeeStatus() != EmployeeStatus.ACTIVE) return Response.error(RouteMessages.EMPLOYEE_INACTIVE);
            if (!Boolean.TRUE.equals(requester.getIsManager())) return Response.error(RouteMessages.UNAUTHORIZED);
            return null;
        });
    }

    private Response validateBase(RouteDTO dto, boolean update) {
        if (dto == null) return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        if (update && isBlank(dto.getId())) return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        if (isBlank(dto.getRouteCode())) return Response.error(RouteMessages.ROUTE_CODE_REQUIRED);
        if (isBlank(dto.getDepartureStationId())) return Response.error(RouteMessages.DEPARTURE_STATION_REQUIRED);
        if (isBlank(dto.getDestinationStationId())) return Response.error(RouteMessages.DESTINATION_STATION_REQUIRED);
        if (dto.getDepartureStationId().equals(dto.getDestinationStationId())) return Response.error(RouteMessages.SAME_STATION);
        if (dto.getPriceBasic() == null || dto.getPriceBasic() < 0) return Response.error(RouteMessages.PRICE_INVALID);
        return null;
    }

    private Response validateAction(RouteActionDTO dto) {
        if (dto == null) return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        if (isBlank(dto.getRouteId())) return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        return null;
    }

    private StopValidation validateStops(jakarta.persistence.EntityManager em,
                                         List<RouteStopDTO> stopDtos,
                                         String departureStationId,
                                         String destinationStationId) {
        if (stopDtos == null || stopDtos.isEmpty()) {
            return new StopValidation(null, List.of());
        }

        List<RouteStopDTO> sortedDtos = new ArrayList<>(stopDtos);
        sortedDtos.sort(Comparator.comparingInt(RouteStopDTO::getOrderStop));
        List<RouteStop> stops = new ArrayList<>();
        Set<String> stationIds = new LinkedHashSet<>();

        for (int i = 0; i < sortedDtos.size(); i++) {
            RouteStopDTO dto = sortedDtos.get(i);
            if (dto.getOrderStop() != i + 1) {
                return new StopValidation(Response.error(RouteMessages.STOP_ORDER_INVALID), List.of());
            }
            if (isBlank(dto.getStationStopId())) {
                return new StopValidation(Response.error(RouteMessages.STATION_STOP_REQUIRED), List.of());
            }
            if (dto.getStationStopId().equals(departureStationId)
                    || dto.getStationStopId().equals(destinationStationId)) {
                return new StopValidation(Response.error(RouteMessages.STOP_MATCHES_ENDPOINT), List.of());
            }
            if (!stationIds.add(dto.getStationStopId())) {
                return new StopValidation(Response.error(RouteMessages.STOP_DUPLICATED), List.of());
            }
            Station station = em.find(Station.class, dto.getStationStopId());
            if (station == null) {
                return new StopValidation(Response.error(RouteMessages.stationNotFound(dto.getStationStopId())), List.of());
            }
            stops.add(RouteStop.builder()
                    .orderStop(i + 1)
                    .stationStop(station)
                    .build());
        }
        return new StopValidation(null, stops);
    }

    private void persistStops(jakarta.persistence.EntityManager em, Route route, List<RouteStop> stops) {
        for (RouteStop stop : stops) {
            stop.setRoute(route);
            routeStopRepository.createRouteStop(em, stop);
        }
    }

    private RouteDTO toDto(Route route) {
        return RouteDTO.builder()
                .id(route.getId())
                .routeCode(route.getRouteCode())
                .departureStationId(route.getDepartureStation() != null ? route.getDepartureStation().getId() : null)
                .departureStationName(route.getDepartureStation() != null ? route.getDepartureStation().getName() : null)
                .destinationStationId(route.getDestinationStation() != null ? route.getDestinationStation().getId() : null)
                .destinationStationName(route.getDestinationStation() != null ? route.getDestinationStation().getName() : null)
                .status(route.getStatus())
                .priceBasic(route.getPriceBasic())
                .routeStops(toStopDtos(route))
                .build();
    }

    private List<RouteStopDTO> toStopDtos(Route route) {
        if (route.getRouteStops() == null) {
            return List.of();
        }
        return route.getRouteStops().stream()
                .sorted(Comparator.comparingInt(RouteStop::getOrderStop))
                .map(stop -> RouteStopDTO.builder()
                        .id(stop.getId())
                        .routeId(route.getId())
                        .orderStop(stop.getOrderStop())
                        .stationStopId(stop.getStationStop() != null ? stop.getStationStop().getId() : null)
                        .stationStopName(stop.getStationStop() != null ? stop.getStationStop().getName() : null)
                        .build())
                .toList();
    }

    private boolean containsStationFilter(Route route, String departureStationId, String destinationStationId) {
        List<String> path = routePathStationIds(route);
        if (!isBlank(departureStationId) && isBlank(destinationStationId)) {
            return path.contains(departureStationId);
        }
        if (isBlank(departureStationId) && !isBlank(destinationStationId)) {
            return path.contains(destinationStationId);
        }
        if (isBlank(departureStationId)) {
            return true;
        }
        int departureIndex = path.indexOf(departureStationId);
        int destinationIndex = path.indexOf(destinationStationId);
        return departureIndex >= 0 && destinationIndex >= 0 && departureIndex < destinationIndex;
    }

    private List<String> routePathStationIds(Route route) {
        List<String> path = new ArrayList<>();
        if (route.getDepartureStation() != null) {
            path.add(route.getDepartureStation().getId());
        }
        if (route.getRouteStops() != null) {
            route.getRouteStops().stream()
                    .sorted(Comparator.comparingInt(RouteStop::getOrderStop))
                    .map(RouteStop::getStationStop)
                    .filter(station -> station != null && station.getId() != null)
                    .map(Station::getId)
                    .forEach(path::add);
        }
        if (route.getDestinationStation() != null) {
            path.add(route.getDestinationStation().getId());
        }
        return path;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record StopValidation(Response error, List<RouteStop> stops) {}

}
