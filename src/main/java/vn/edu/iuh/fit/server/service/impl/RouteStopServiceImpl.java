package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.dto.RouteActionDTO;
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
import vn.edu.iuh.fit.server.service.RouteStopService;

import java.util.List;

public class RouteStopServiceImpl implements RouteStopService {

    private static final Logger log = LoggerFactory.getLogger(RouteStopServiceImpl.class);

    private final RouteStopRepository repository = new RouteStopRepositoryImpl();
    private final RouteRepository routeRepository = new RouteRepositoryImpl();
    private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();

    @Override
    public Response findRouteStopsByRouteId(RouteActionDTO dto) {
        if (dto == null || isBlank(dto.getRouteId())) {
            return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        }
        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                Route route = routeRepository.findRouteById(em, dto.getRouteId());
                if (route == null) return Response.error(RouteMessages.notFound(dto.getRouteId()));
                List<RouteStopDTO> stops = repository.findRouteStopsByRouteId(em, dto.getRouteId()).stream()
                        .map(this::toDto)
                        .toList();
                return Response.success(RouteMessages.FIND_STOPS_SUCCESS, stops);
            });
        } catch (RuntimeException e) {
            log.error("Failed to find route stops: routeId={}", dto.getRouteId(), e);
            return Response.error(RouteMessages.ROUTE_STOP_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response createRouteStop(RouteStopDTO dto) {
        Response validation = validateDto(dto, false);
        if (validation != null) return validation;
        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Route route = routeRepository.findRouteById(em, dto.getRouteId());
                if (route == null) return Response.error(RouteMessages.notFound(dto.getRouteId()));
                if (routeRepository.hasSchedules(em, dto.getRouteId())) return Response.error(RouteMessages.HAS_SCHEDULES_UPDATE);
                Response routeStopError = validateRouteStopAgainstRoute(em, route, dto, null);
                if (routeStopError != null) return routeStopError;

                RouteStop stop = RouteStop.builder()
                        .route(route)
                        .stationStop(em.getReference(Station.class, dto.getStationStopId()))
                        .orderStop(dto.getOrderStop())
                        .build();
                repository.createRouteStop(em, stop);
                return Response.success(RouteMessages.CREATE_STOP_SUCCESS, stop.getId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to create route stop: routeId={}", dto.getRouteId(), e);
            return Response.error(RouteMessages.ROUTE_STOP_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response updateRouteStop(RouteStopDTO dto) {
        Response validation = validateDto(dto, true);
        if (validation != null) return validation;
        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                RouteStop existing = repository.findRouteStopById(em, dto.getId());
                if (existing == null) return Response.error(RouteMessages.notFound(dto.getId()));
                Route route = existing.getRoute();
                if (route == null) return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
                if (routeRepository.hasSchedules(em, route.getId())) return Response.error(RouteMessages.HAS_SCHEDULES_UPDATE);
                Response routeStopError = validateRouteStopAgainstRoute(em, route, dto, existing.getId());
                if (routeStopError != null) return routeStopError;

                existing.setStationStop(em.getReference(Station.class, dto.getStationStopId()));
                existing.setOrderStop(dto.getOrderStop());
                repository.updateRouteStop(em, existing);
                return Response.success(RouteMessages.UPDATE_STOP_SUCCESS, existing.getId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to update route stop: id={}", dto.getId(), e);
            return Response.error(RouteMessages.ROUTE_STOP_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response deleteRouteStop(RouteStopDTO dto) {
        if (dto == null || isBlank(dto.getId())) {
            return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        }
        Response authError = requireActiveManager(dto.getRequestEmployeeId());
        if (authError != null) return authError;

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                RouteStop existing = repository.findRouteStopById(em, dto.getId());
                if (existing == null) return Response.error(RouteMessages.notFound(dto.getId()));
                Route route = existing.getRoute();
                if (route != null && routeRepository.hasSchedules(em, route.getId())) {
                    return Response.error(RouteMessages.HAS_SCHEDULES_UPDATE);
                }
                repository.deleteRouteStop(em, dto.getId());
                return Response.success(RouteMessages.DELETE_STOP_SUCCESS, dto.getId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to delete route stop: id={}", dto.getId(), e);
            return Response.error(RouteMessages.ROUTE_STOP_FAILED_PREFIX + e.getMessage());
        }
    }

    private Response validateDto(RouteStopDTO dto, boolean update) {
        if (dto == null) return Response.error(RouteMessages.STATION_STOP_REQUIRED);
        if (update && isBlank(dto.getId())) return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        if (!update && isBlank(dto.getRouteId())) return Response.error(RouteMessages.ROUTE_ID_REQUIRED);
        if (isBlank(dto.getStationStopId())) return Response.error(RouteMessages.STATION_STOP_REQUIRED);
        if (dto.getOrderStop() <= 0) return Response.error(RouteMessages.STOP_ORDER_INVALID);
        return null;
    }

    private Response validateRouteStopAgainstRoute(jakarta.persistence.EntityManager em,
                                                   Route route,
                                                   RouteStopDTO dto,
                                                   String ignoreStopId) {
        if (route.getDepartureStation() != null && dto.getStationStopId().equals(route.getDepartureStation().getId())) {
            return Response.error(RouteMessages.STOP_MATCHES_ENDPOINT);
        }
        if (route.getDestinationStation() != null && dto.getStationStopId().equals(route.getDestinationStation().getId())) {
            return Response.error(RouteMessages.STOP_MATCHES_ENDPOINT);
        }
        if (em.find(Station.class, dto.getStationStopId()) == null) {
            return Response.error(RouteMessages.stationNotFound(dto.getStationStopId()));
        }
        boolean duplicate = repository.findRouteStopsByRouteId(em, route.getId()).stream()
                .filter(stop -> ignoreStopId == null || !ignoreStopId.equals(stop.getId()))
                .anyMatch(stop -> stop.getOrderStop() == dto.getOrderStop()
                        || (stop.getStationStop() != null
                        && dto.getStationStopId().equals(stop.getStationStop().getId())));
        if (duplicate) {
            return Response.error(RouteMessages.STOP_DUPLICATED);
        }
        return null;
    }

    private RouteStopDTO toDto(RouteStop stop) {
        return RouteStopDTO.builder()
                .id(stop.getId())
                .routeId(stop.getRoute() != null ? stop.getRoute().getId() : null)
                .orderStop(stop.getOrderStop())
                .stationStopId(stop.getStationStop() != null ? stop.getStationStop().getId() : null)
                .stationStopName(stop.getStationStop() != null ? stop.getStationStop().getName() : null)
                .build();
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
