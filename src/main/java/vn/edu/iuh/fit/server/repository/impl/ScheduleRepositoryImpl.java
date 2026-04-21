package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.constant.StatusSchedule;
import vn.edu.iuh.fit.server.model.Route;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class ScheduleRepositoryImpl extends AbstractGenericRepositoryImpl<Schedule, String> implements ScheduleRepository {

    public ScheduleRepositoryImpl() {
        super(Schedule.class);
    }

    @Override
    public Schedule createScheduleWithDetails(Schedule schedule, String trainId) {
        return doInTransaction(em -> {
            schedule.setTrain(em.getReference(Train.class, schedule.getTrain().getId()));
            schedule.setRoute(em.getReference(Route.class, schedule.getRoute().getId()));
            em.persist(schedule);
            generateScheduleDetails(em, schedule, trainId);
            return schedule;
        });
    }

    @Override
    public boolean updateSchedule(Schedule schedule) {
        return doInTransaction(em -> {
            Schedule existingSchedule = em.find(Schedule.class, schedule.getId());
            if (existingSchedule == null) {
                return false;
            }

            String existingTrainId = existingSchedule.getTrain() != null ? existingSchedule.getTrain().getId() : null;
            String existingRouteId = existingSchedule.getRoute() != null ? existingSchedule.getRoute().getId() : null;
            String incomingTrainId = schedule.getTrain() != null ? schedule.getTrain().getId() : null;
            String incomingRouteId = schedule.getRoute() != null ? schedule.getRoute().getId() : null;

            boolean trainChanged = !Objects.equals(existingTrainId, incomingTrainId);
            boolean routeChanged = !Objects.equals(existingRouteId, incomingRouteId);

            existingSchedule.setDepartureTime(schedule.getDepartureTime());
            existingSchedule.setArrivalTime(schedule.getArrivalTime());
            existingSchedule.setTrain(em.getReference(Train.class, incomingTrainId));
            existingSchedule.setRoute(em.getReference(Route.class, incomingRouteId));

            if (trainChanged || routeChanged) {
                deleteScheduleDetailsByScheduleId(em, existingSchedule.getId());
                generateScheduleDetails(em, existingSchedule, incomingTrainId);
            }

            return true;
        });
    }

    @Override
    public boolean deleteSchedule(String scheduleId) {
        return doInTransaction(em -> {
            Schedule existingSchedule = em.find(Schedule.class, scheduleId);
            if (existingSchedule == null) {
                return false;
            }

            deleteScheduleDetailsByScheduleId(em, scheduleId);
            em.remove(existingSchedule);
            return true;
        });
    }

    @Override
    public Schedule findScheduleById(String scheduleId) {
        return doWithEntityManager(em ->
                em.createQuery(
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
                        .orElse(null)
        );
    }

    @Override
    public List<Schedule> findAllSchedules() {
        return doWithEntityManager(em ->
                em.createQuery(
                                "SELECT s FROM Schedule s " +
                                        "JOIN FETCH s.route r " +
                                        "JOIN FETCH r.departureStation " +
                                        "JOIN FETCH r.destinationStation " +
                                        "JOIN FETCH s.train " +
                                        "ORDER BY s.departureTime DESC",
                                Schedule.class)
                        .getResultList()
        );
    }

    @Override
    public List<Schedule> searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime, String status) {
        return doWithEntityManager(em -> {
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
        });
    }

    @Override
    public List<Schedule> findSchedulesByStationIds(String departureStationId, String destinationStationId) {
        return doWithEntityManager(em ->
                em.createQuery(
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
                        .getResultList()
        );
    }

    @Override
    public List<Schedule> filterSchedules(ScheduleFilterDTO filter) {
        return doWithEntityManager(em -> {
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
        });
    }

    private void deleteScheduleDetailsByScheduleId(EntityManager entityManager, String scheduleId) {
        entityManager.createQuery("DELETE FROM ScheduleDetail sd WHERE sd.schedule.id = :scheduleId")
                .setParameter("scheduleId", scheduleId)
                .executeUpdate();
    }

    private void generateScheduleDetails(EntityManager entityManager, Schedule schedule, String trainId) {
        List<Seat> seats = entityManager.createQuery(
                        "SELECT s FROM Seat s WHERE s.carriage.train.id = :trainId",
                        Seat.class)
                .setParameter("trainId", trainId)
                .getResultList();

        int batchSize = 50;
        for (int index = 0; index < seats.size(); index++) {
            Seat seat = seats.get(index);
            ScheduleDetail scheduleDetail = ScheduleDetail.builder()
                    .schedule(schedule)
                    .seat(seat)
                    .priceSeat(BigDecimal.ZERO)
                    .routeStop(null)
                    .build();
            entityManager.persist(scheduleDetail);

            if (index > 0 && index % batchSize == 0) {
                entityManager.flush();
            }
        }
    }
}
