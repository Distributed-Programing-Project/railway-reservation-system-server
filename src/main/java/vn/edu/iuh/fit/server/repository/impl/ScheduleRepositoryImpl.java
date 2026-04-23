package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.StatusSchedule;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.common.dto.ScheduleFilterDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class ScheduleRepositoryImpl extends AbstractGenericRepositoryImpl<Schedule, String> implements ScheduleRepository {

    public ScheduleRepositoryImpl() {
        super(Schedule.class);
    }

    @Override
    public Schedule createScheduleWithDetails(EntityManager em, Schedule schedule, String trainId) {
        schedule.setTrain(em.getReference(Train.class, schedule.getTrain().getId()));
        schedule.setRoute(em.getReference(Route.class, schedule.getRoute().getId()));
        em.persist(schedule);
        String scheduleId = schedule.getId();

        List<String> seatIds = em.createQuery(
                "SELECT s.id FROM Seat s WHERE s.carriage.train.id = :trainId", String.class)
                .setParameter("trainId", trainId)
                .getResultList();

        int batchSize = 50;
        for (int i = 0; i < seatIds.size(); i++) {
            ScheduleDetail detail = ScheduleDetail.builder()
                    .schedule(em.getReference(Schedule.class, scheduleId))
                    .seat(em.getReference(Seat.class, seatIds.get(i)))
                    .priceSeat(BigDecimal.ZERO)
                    .routeStop(null)
                    .build();
            em.persist(detail);

            if ((i + 1) % batchSize == 0) {
                em.flush();
                em.clear();
            }
        }

        return em.getReference(Schedule.class, scheduleId);
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
            createScheduleDetails(em, existingSchedule.getId(), incomingTrainId);
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
        return em.createQuery(
                        "SELECT s FROM Schedule s " +
                                "JOIN FETCH s.route r " +
                                "JOIN FETCH r.departureStation dep " +
                                "JOIN FETCH r.destinationStation dest " +
                                "JOIN FETCH s.train " +
                                "WHERE dep.id = :departureStationId " +
                                "AND dest.id = :destinationStationId " +
                                "ORDER BY s.departureTime DESC",
                        Schedule.class)
                .setParameter("departureStationId", departureStationId)
                .setParameter("destinationStationId", destinationStationId)
                .getResultList();
    }

    @Override
    public List<Schedule> filterSchedules(EntityManager em, ScheduleFilterDTO filter) {
        StringBuilder jpql = new StringBuilder(
                "SELECT s FROM Schedule s JOIN FETCH s.route r JOIN FETCH r.departureStation JOIN FETCH r.destinationStation JOIN FETCH s.train t WHERE 1=1 ");
        if (filter.getDepartureStationId() != null && !filter.getDepartureStationId().isEmpty()) {
            jpql.append("AND r.departureStation.id = :depId ");
        }
        if (filter.getDestinationStationId() != null && !filter.getDestinationStationId().isEmpty()) {
            jpql.append("AND r.destinationStation.id = :destId ");
        }
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

        if (filter.getDepartureStationId() != null && !filter.getDepartureStationId().isEmpty()) {
            query.setParameter("depId", filter.getDepartureStationId());
        }
        if (filter.getDestinationStationId() != null && !filter.getDestinationStationId().isEmpty()) {
            query.setParameter("destId", filter.getDestinationStationId());
        }
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

        query.setFirstResult(filter.getPage() * filter.getSize());
        query.setMaxResults(filter.getSize());

        return query.getResultList();
    }

    private void deleteScheduleDetailsByScheduleId(EntityManager em, String scheduleId) {
        em.createQuery("DELETE FROM ScheduleDetail sd WHERE sd.schedule.id = :scheduleId")
                .setParameter("scheduleId", scheduleId)
                .executeUpdate();
    }

    private void createScheduleDetails(EntityManager em, String scheduleId, String trainId) {
        List<String> seatIds = em.createQuery(
                        "SELECT s.id FROM Seat s WHERE s.carriage.train.id = :trainId", String.class)
                .setParameter("trainId", trainId)
                .getResultList();

        int batchSize = 50;
        for (int i = 0; i < seatIds.size(); i++) {
            ScheduleDetail detail = ScheduleDetail.builder()
                    .schedule(em.getReference(Schedule.class, scheduleId))
                    .seat(em.getReference(Seat.class, seatIds.get(i)))
                    .priceSeat(BigDecimal.ZERO)
                    .routeStop(null)
                    .build();
            em.persist(detail);

            if ((i + 1) % batchSize == 0) {
                em.flush();
                em.clear();
            }
        }
    }

    @Override
    public long countFutureActiveSchedulesByTrainId(String trainId) {
        return doWithEntityManager(em ->
                em.createQuery(
                        "SELECT COUNT(s) FROM Schedule s WHERE s.train.id = :trainId" +
                        " AND s.departureTime > :now" +
                        " AND s.status NOT IN :terminalStatuses",
                        Long.class)
                        .setParameter("trainId", trainId)
                        .setParameter("now", LocalDateTime.now())
                        .setParameter("terminalStatuses", List.of(StatusSchedule.COMPLETED, StatusSchedule.CANCELLED))
                        .getSingleResult()
        );
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

}
