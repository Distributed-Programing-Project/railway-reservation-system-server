package vn.edu.iuh.fit.server.repository.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.constant.TicketStatus;
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
    public Set<String> getSoldSeatIds(EntityManager em, String scheduleId) {
        // A seat (ScheduleDetail) is sold if there's a Ticket for it that is not
        // CANCELLED
        String jpql = "SELECT sd.seat.id FROM Ticket t JOIN t.scheduleDetail sd " +
                "WHERE sd.schedule.id = :scheduleId AND t.status != :cancelledStatus";

        List<String> seatIds = em.createQuery(jpql, String.class)
                .setParameter("scheduleId", scheduleId)
                .setParameter("cancelledStatus", TicketStatus.CANCELLED)
                .getResultList();

        return new HashSet<>(seatIds);
    }
}
