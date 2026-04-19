package vn.edu.iuh.fit.server.controller;

import java.util.Set;

public interface ScheduleDetailController {
    Set<Integer> getSoldSeatIds(String scheduleId);

    int createScheduleDetail(String scheduleId, int seatId, double seatPrice, String status);

    boolean updateSeatStatus(int scheduleDetailId, String newStatus);
}

