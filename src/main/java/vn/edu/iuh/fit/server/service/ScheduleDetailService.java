package vn.edu.iuh.fit.server.service;

import java.util.Set;

public interface ScheduleDetailService {
    Set<Integer> getSoldSeatIds(String scheduleId);

    int createScheduleDetail(String scheduleId, int seatId, double seatPrice, String status);

    boolean updateSeatStatus(int scheduleDetailId, String newStatus);
}

