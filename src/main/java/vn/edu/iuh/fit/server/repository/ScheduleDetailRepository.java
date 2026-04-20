package vn.edu.iuh.fit.server.repository;

import java.util.Set;

public interface ScheduleDetailRepository {
    /**
     * Get the set of Seat IDs that are already booked/sold for a given schedule.
     * A seat is considered sold if there is an active Ticket (status != CANCELLED) for it.
     */
    Set<String> getSoldSeatIds(String scheduleId);
}
