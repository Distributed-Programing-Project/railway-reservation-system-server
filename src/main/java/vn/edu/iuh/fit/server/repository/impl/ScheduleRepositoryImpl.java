package vn.edu.iuh.fit.server.repository.impl;

import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.dto.ScheduleFilterDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ScheduleRepositoryImpl extends AbstractGenericRepositoryImpl<Schedule, String> implements ScheduleRepository {

    public ScheduleRepositoryImpl() {
        super(Schedule.class);
    }

    @Override
    public Schedule createScheduleWithDetails(Schedule schedule, String trainId) {
        return doInTransaction(em -> {
            em.persist(schedule);

            // Fetch all seats for the given train
            List<Seat> seats = em.createQuery("SELECT s FROM Seat s WHERE s.carriage.train.id = :trainId", Seat.class)
                    .setParameter("trainId", trainId)
                    .getResultList();

            // Batch insert ScheduleDetails (avoid OOM)
            int batchSize = 50;
            for (int i = 0; i < seats.size(); i++) {
                ScheduleDetail detail = ScheduleDetail.builder()
                        .schedule(schedule)
                        .seat(seats.get(i))
                        .priceSeat(BigDecimal.ZERO)
                        .routeStop(null)
                        .build();
                em.persist(detail);

                if (i > 0 && i % batchSize == 0) {
                    em.flush();
                    em.clear();
                }
            }

            return schedule;
        });
    }

    @Override
    public boolean updateSchedule(Schedule schedule) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean deleteSchedule(String scheduleId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Schedule findScheduleById(String scheduleId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Schedule> findAllSchedules() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Schedule> searchSchedules(String routeId, String trainId, LocalDateTime fromDateTime, LocalDateTime toDateTime, String status) {

        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Schedule> findSchedulesByStationIds(String departureStationId, String destinationStationId) {
        throw new UnsupportedOperationException("Not implemented yet");
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
}
