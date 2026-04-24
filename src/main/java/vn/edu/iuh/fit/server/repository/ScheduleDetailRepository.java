package vn.edu.iuh.fit.server.repository;

import java.util.Set;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.ScheduleDetail;

public interface ScheduleDetailRepository {
    /**
     * Get the set of Seat IDs that are already booked/sold for a given schedule.
     * A seat is considered sold if there is an active Ticket (status != CANCELLED)
     * for it.
     */
    Set<String> getSoldSeatIds(EntityManager em, String scheduleId);

    /**
     * Get the set of Seat IDs that are booked/sold for a given schedule,
     * using pessimistic lock (SELECT FOR UPDATE) to prevent race conditions
     * during concurrent ticket booking.
     * Caller must ensure the EntityManager has an active transaction.
     */
    Set<String> getSoldSeatIdsWithLock(EntityManager em, String scheduleId);

    ScheduleDetail findById(String id, EntityManager em);

    ScheduleDetail updateScheduleDetail(EntityManager em, ScheduleDetail scheduleDetail);

    boolean existsUnpricedSeat(EntityManager em, String scheduleId);
}
