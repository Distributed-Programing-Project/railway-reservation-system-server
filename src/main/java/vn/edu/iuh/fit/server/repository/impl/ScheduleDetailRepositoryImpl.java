package vn.edu.iuh.fit.server.repository.impl;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.repository.ScheduleDetailRepository;

public class ScheduleDetailRepositoryImpl extends AbstractGenericRepositoryImpl<ScheduleDetail, String>
        implements ScheduleDetailRepository {

    public ScheduleDetailRepositoryImpl() {
        super(ScheduleDetail.class);
    }

    @Override
    public ScheduleDetail findById(String id, EntityManager em) {
        return em.find(ScheduleDetail.class, id);
    }

    @Override
    public ScheduleDetail updateScheduleDetail(EntityManager em, ScheduleDetail scheduleDetail) {
        return em.merge(scheduleDetail);
    }

  @Override
  public Set<String> getSoldSeatIds(EntityManager em, String scheduleId) {
    String jpql = "SELECT sd.seat.id FROM Ticket t JOIN t.scheduleDetail sd " +
            "WHERE sd.schedule.id = :scheduleId AND t.status NOT IN (:cancelledStatus, :exchangedStatus, :returnedStatus)";

    List<String> seatIds = em.createQuery(jpql, String.class)
            .setParameter("scheduleId", scheduleId)
            .setParameter("cancelledStatus", TicketStatus.CANCELLED)
            .setParameter("exchangedStatus", TicketStatus.EXCHANGED)
            .setParameter("returnedStatus", TicketStatus.RETURNED)
            .getResultList();

        return new HashSet<>(seatIds);
    }

    @Override
    public Set<String> getSoldSeatIdsWithLock(EntityManager em, String scheduleId) {
        List<String> seatIds = em.createQuery(
                "SELECT sd.seat.id FROM ScheduleDetail sd WHERE sd.schedule.id = :scheduleId",
                String.class)
                .setParameter("scheduleId", scheduleId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        return new HashSet<>(seatIds);
    }

    @Override
    public boolean existsUnpricedSeat(EntityManager em, String scheduleId) {
        Long count = em.createQuery(
                        "SELECT COUNT(sd) FROM ScheduleDetail sd " +
                        "WHERE sd.schedule.id = :scheduleId " +
                        "AND (sd.priceSeat IS NULL OR sd.priceSeat <= :zero)",
                        Long.class)
                .setParameter("scheduleId", scheduleId)
                .setParameter("zero", BigDecimal.ZERO)
                .getSingleResult();
        return count != null && count > 0;
    }
}
