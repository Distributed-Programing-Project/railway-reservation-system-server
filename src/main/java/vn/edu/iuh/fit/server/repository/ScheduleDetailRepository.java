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

    ScheduleDetail findById(String id, EntityManager em);
}
