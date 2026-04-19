package vn.edu.iuh.fit.server.controller.impl;

import vn.edu.iuh.fit.server.controller.ScheduleDetailController;

import java.util.Set;

public class ScheduleDetailControllerImpl implements ScheduleDetailController {
    @Override
    public Set<Integer> getSoldSeatIds(String scheduleId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int createScheduleDetail(String scheduleId, int seatId, double seatPrice, String status) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean updateSeatStatus(int scheduleDetailId, String newStatus) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

