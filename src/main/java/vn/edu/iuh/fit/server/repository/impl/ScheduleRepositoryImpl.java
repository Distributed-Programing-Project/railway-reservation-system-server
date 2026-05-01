package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.RouteStatus;
import vn.edu.iuh.fit.common.constant.StatusSchedule;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.common.dto.ScheduleFilterDTO;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.RouteStop;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScheduleRepositoryImpl extends AbstractGenericRepositoryImpl<Schedule, String> implements ScheduleRepository {

    private static final int BATCH_SIZE = 50;

    public ScheduleRepositoryImpl() {
        super(Schedule.class);
    }

    @Override
    public Schedule createScheduleWithDetails(EntityManager em, Schedule schedule, String trainId) {
        schedule.setTrain(em.getReference(Train.class, schedule.getTrain().getId()));
        schedule.setRoute(em.getReference(Route.class, schedule.getRoute().getId()));
        em.persist(schedule);
        createScheduleDetails(em, schedule.getId(), trainId, schedule.getRoute().getId());
        return em.getReference(Schedule.class, schedule.getId());
    }

    @Override
    public boolean updateSchedule(EntityManager em, Schedule schedule) {
        Schedule existingSchedule = em.find(Schedule.class, schedule.getId());
        if (existingSchedule == null) {
            return false;
        }

        String existingTrainId = existingSchedule.getTrain() != null ? existingSchedule.getTrain().getId() : null;
        String incomingTrainId = schedule.getTrain() != null ? schedule.getTrain().getId() : null;

        boolean trainChanged = !Objects.equals(existingTrainId, incomingTrainId);
        boolean routeChanged = !Objects.equals(
                existingSchedule.getRoute() != null ? existingSchedule.getRoute().getId() : null,
                schedule.getRoute() != null ? schedule.getRoute().getId() : null
        );

        existingSchedule.setDepartureTime(schedule.getDepartureTime());
        existingSchedule.setArrivalTime(schedule.getArrivalTime());
        existingSchedule.setTrain(em.getReference(Train.class, incomingTrainId));
        existingSchedule.setRoute(em.getReference(Route.class, schedule.getRoute().getId()));

        if (trainChanged || routeChanged) {
            deleteScheduleDetailsByScheduleId(em, existingSchedule.getId());
            createScheduleDetails(em, existingSchedule.getId(), incomingTrainId, schedule.getRoute().getId());
        }

        return true;
    }

    @Override
    public boolean deleteSchedule(EntityManager em, String scheduleId) {
        Schedule existingSchedule = em.find(Schedule.class, scheduleId);
        if (existingSchedule == null) {
            return false;
        }
        deleteScheduleDetailsByScheduleId(em, scheduleId);
        em.remove(existingSchedule);
        return true;
    }

    @Override
    public Schedule findScheduleById(EntityManager em, String scheduleId) {
        return em.createQuery(
                        "SELECT s FROM Schedule s " +
                                "JOIN FETCH s.route r " +
                                "JOIN FETCH r.departureStation " +
                                "JOIN FETCH r.destinationStation " +
                                "JOIN FETCH s.train " +
                                "WHERE s.id = :scheduleId",
                        Schedule.class)
                .setParameter("scheduleId", scheduleId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Schedule> findAllSchedules(EntityManager em) {
        return em.createQuery(
                        "SELECT s FROM Schedule s " +
                                "JOIN FETCH s.route r " +
                                "JOIN FETCH r.departureStation " +
                                "JOIN FETCH r.destinationStation " +
                                "JOIN FETCH s.train " +
                                "ORDER BY s.departureTime DESC",
                        Schedule.class)
                .getResultList();
    }

    @Override
    public List<Schedule> searchSchedules(EntityManager em, String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime, String status) {
        StringBuilder jpql = new StringBuilder(
                "SELECT s FROM Schedule s " +
                        "JOIN FETCH s.route r " +
                        "JOIN FETCH r.departureStation " +
                        "JOIN FETCH r.destinationStation " +
                        "JOIN FETCH s.train t " +
                        "WHERE 1=1 "
        );

        if (routeId != null && !routeId.isBlank()) {
            jpql.append("AND r.id = :routeId ");
        }
        if (trainId != null && !trainId.isBlank()) {
            jpql.append("AND t.id = :trainId ");
        }
        if (fromDateTime != null) {
            jpql.append("AND s.departureTime >= :fromDateTime ");
        }
        if (toDateTime != null) {
            jpql.append("AND s.departureTime <= :toDateTime ");
        }
        if (status != null && !status.isBlank()) {
            jpql.append("AND s.status = :status ");
        }
        jpql.append("ORDER BY s.departureTime DESC");

        var query = em.createQuery(jpql.toString(), Schedule.class);
        if (routeId != null && !routeId.isBlank()) {
            query.setParameter("routeId", routeId);
        }
        if (trainId != null && !trainId.isBlank()) {
            query.setParameter("trainId", trainId);
        }
        if (fromDateTime != null) {
            query.setParameter("fromDateTime", fromDateTime);
        }
        if (toDateTime != null) {
            query.setParameter("toDateTime", toDateTime);
        }
        if (status != null && !status.isBlank()) {
            query.setParameter("status", StatusSchedule.valueOf(status.trim().toUpperCase()));
        }
        return query.getResultList();
    }

    @Override
    public List<Schedule> findSchedulesByStationIds(EntityManager em, String departureStationId, String destinationStationId) {
        List<Schedule> schedules = em.createQuery(
                        "SELECT DISTINCT s FROM Schedule s " +
                                "JOIN FETCH s.route r " +
                                "JOIN FETCH r.departureStation dep " +
                                "JOIN FETCH r.destinationStation dest " +
                                "LEFT JOIN FETCH r.routeStops rs " +
                                "LEFT JOIN FETCH rs.stationStop " +
                                "JOIN FETCH s.train " +
                                "ORDER BY s.departureTime DESC",
                        Schedule.class)
                .getResultList();
        return schedules.stream()
                .filter(schedule -> routeContainsOrderedStations(schedule.getRoute(), departureStationId, destinationStationId))
                .toList();
    }

    @Override
    public List<Schedule> filterSchedules(EntityManager em, ScheduleFilterDTO filter) {
        StringBuilder jpql = new StringBuilder(
                "SELECT DISTINCT s FROM Schedule s " +
                        "JOIN FETCH s.route r " +
                        "JOIN FETCH r.departureStation " +
                        "JOIN FETCH r.destinationStation " +
                        "LEFT JOIN FETCH r.routeStops rs " +
                        "LEFT JOIN FETCH rs.stationStop " +
                        "JOIN FETCH s.train t WHERE 1=1 ");
        if (filter.getTrainId() != null && !filter.getTrainId().isEmpty()) {
            jpql.append("AND t.id = :trainId ");
        }
        if (filter.getStatus() != null) {
            jpql.append("AND s.status = :status ");
        }
        if (filter.getFromDate() != null) {
            jpql.append("AND s.departureTime >= :fromDate ");
        }
        if (filter.getToDate() != null) {
            jpql.append("AND s.departureTime <= :toDate ");
        }
        jpql.append("ORDER BY s.departureTime DESC");

        var query = em.createQuery(jpql.toString(), Schedule.class);

        if (filter.getTrainId() != null && !filter.getTrainId().isEmpty()) {
            query.setParameter("trainId", filter.getTrainId());
        }
        if (filter.getStatus() != null) {
            query.setParameter("status", filter.getStatus());
        }
        if (filter.getFromDate() != null) {
            query.setParameter("fromDate", filter.getFromDate().atStartOfDay());
        }
        if (filter.getToDate() != null) {
            query.setParameter("toDate", filter.getToDate().plusDays(1).atStartOfDay().minusSeconds(1));
        }

        List<Schedule> schedules = query.getResultList().stream()
                .filter(schedule -> routeContainsOrderedStations(schedule.getRoute(),
                        filter.getDepartureStationId(), filter.getDestinationStationId()))
                .toList();
        int fromIndex = Math.min(filter.getPage() * filter.getSize(), schedules.size());
        int toIndex = Math.min(fromIndex + filter.getSize(), schedules.size());
        return schedules.subList(fromIndex, toIndex);
    }

    private void deleteScheduleDetailsByScheduleId(EntityManager em, String scheduleId) {
        em.createQuery("DELETE FROM ScheduleDetail sd WHERE sd.schedule.id = :scheduleId")
                .setParameter("scheduleId", scheduleId)
                .executeUpdate();
    }

    private void createScheduleDetails(EntityManager em, String scheduleId, String trainId, String routeId) {
        List<String> seatIds = em.createQuery(
                        "SELECT s.id FROM Seat s WHERE s.carriage.train.id = :trainId", String.class)
                .setParameter("trainId", trainId)
                .getResultList();

        List<RoutePathStation> path = buildRoutePath(em, routeId);
        int persisted = 0;
        for (String seatId : seatIds) {
            for (int departureIndex = 0; departureIndex < path.size() - 1; departureIndex++) {
                for (int destinationIndex = departureIndex + 1; destinationIndex < path.size(); destinationIndex++) {
                    RoutePathStation departure = path.get(departureIndex);
                    RoutePathStation destination = path.get(destinationIndex);
                    ScheduleDetail detail = ScheduleDetail.builder()
                            .schedule(em.getReference(Schedule.class, scheduleId))
                            .seat(em.getReference(Seat.class, seatId))
                            .priceSeat(BigDecimal.ZERO)
                            .routeStop(null)
                            .segmentDepartureStation(em.getReference(Station.class, departure.stationId()))
                            .segmentDestinationStation(em.getReference(Station.class, destination.stationId()))
                            .segmentDepartureOrder(departure.order())
                            .segmentDestinationOrder(destination.order())
                            .build();
                    em.persist(detail);
                    persisted++;

                    if (persisted % BATCH_SIZE == 0) {
                        em.flush();
                        em.clear();
                    }
                }
            }
        }
    }

    private List<RoutePathStation> buildRoutePath(EntityManager em, String routeId) {
        Route route = em.createQuery(
                        "SELECT DISTINCT r FROM Route r " +
                                "JOIN FETCH r.departureStation " +
                                "JOIN FETCH r.destinationStation " +
                                "LEFT JOIN FETCH r.routeStops rs " +
                                "LEFT JOIN FETCH rs.stationStop " +
                                "WHERE r.id = :routeId",
                        Route.class)
                .setParameter("routeId", routeId)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tuyến: id=" + routeId));

        List<RoutePathStation> path = new ArrayList<>();
        path.add(new RoutePathStation(route.getDepartureStation().getId(), 0));
        if (route.getRouteStops() != null) {
            route.getRouteStops().stream()
                    .sorted(Comparator.comparingInt(RouteStop::getOrderStop))
                    .map(RouteStop::getStationStop)
                    .filter(Objects::nonNull)
                    .forEach(station -> path.add(new RoutePathStation(station.getId(), path.size())));
        }
        path.add(new RoutePathStation(route.getDestinationStation().getId(), path.size()));
        return path;
    }

    private record RoutePathStation(String stationId, int order) {}

    private boolean routeContainsOrderedStations(Route route, String departureStationId, String destinationStationId) {
        List<String> path = routePathStationIds(route);
        if ((departureStationId == null || departureStationId.isBlank())
                && (destinationStationId == null || destinationStationId.isBlank())) {
            return true;
        }
        if (departureStationId != null && !departureStationId.isBlank()
                && (destinationStationId == null || destinationStationId.isBlank())) {
            return path.contains(departureStationId);
        }
        if ((departureStationId == null || departureStationId.isBlank())
                && destinationStationId != null && !destinationStationId.isBlank()) {
            return path.contains(destinationStationId);
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
                    .filter(Objects::nonNull)
                    .map(Station::getId)
                    .forEach(path::add);
        }
        if (route.getDestinationStation() != null) {
            path.add(route.getDestinationStation().getId());
        }
        return path;
    }

    @Override
    public long countFutureActiveSchedulesByTrainId(EntityManager em, String trainId) {
        Long count = em.createQuery(
                        "SELECT COUNT(s) FROM Schedule s WHERE s.train.id = :trainId" +
                        " AND s.departureTime > :now" +
                        " AND s.status NOT IN :terminalStatuses",
                        Long.class)
                        .setParameter("trainId", trainId)
                        .setParameter("now", LocalDateTime.now())
                        .setParameter("terminalStatuses", List.of(StatusSchedule.COMPLETED, StatusSchedule.CANCELLED))
                        .getSingleResult();
        return count != null ? count : 0L;
    }

    @Override
    public boolean updateScheduleStatus(EntityManager em, String scheduleId, StatusSchedule status) {
        int updated = em.createQuery(
                        "UPDATE Schedule s SET s.status = :status WHERE s.id = :scheduleId")
                .setParameter("status", status)
                .setParameter("scheduleId", scheduleId)
                .executeUpdate();
        return updated > 0;
    }

    @Override
    public long countSoldSeatsByScheduleId(EntityManager em, String scheduleId) {
        Long count = em.createQuery(
                        "SELECT COUNT(t) FROM Ticket t " +
                        "JOIN t.scheduleDetail sd " +
                        "WHERE sd.schedule.id = :scheduleId " +
                        "AND t.status NOT IN (:cancelled, :exchanged, :returned)",
                        Long.class)
                .setParameter("scheduleId", scheduleId)
                .setParameter("cancelled", TicketStatus.CANCELLED)
                .setParameter("exchanged", TicketStatus.EXCHANGED)
                .setParameter("returned", TicketStatus.RETURNED)
                .getSingleResult();
        return count != null ? count : 0L;
    }

    @Override
    public Route findActiveReverseRoute(EntityManager em, String routeId) {
        Route route = em.createQuery(
                        "SELECT r FROM Route r " +
                                "JOIN FETCH r.departureStation dep " +
                                "JOIN FETCH r.destinationStation dest " +
                                "WHERE r.id = :routeId",
                        Route.class)
                .setParameter("routeId", routeId)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (route == null) {
            return null;
        }
        return em.createQuery(
                        "SELECT r FROM Route r " +
                                "JOIN FETCH r.departureStation dep " +
                                "JOIN FETCH r.destinationStation dest " +
                                "WHERE dep.id = :reverseDepartureId " +
                                "AND dest.id = :reverseDestinationId " +
                                "AND r.status = :activeStatus",
                        Route.class)
                .setParameter("reverseDepartureId", route.getDestinationStation().getId())
                .setParameter("reverseDestinationId", route.getDepartureStation().getId())
                .setParameter("activeStatus", RouteStatus.ACTIVE)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
