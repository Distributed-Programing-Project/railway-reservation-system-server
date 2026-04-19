package vn.edu.iuh.fit.server.service.impl;

import vn.edu.iuh.fit.server.service.ScheduleDetailService;

import java.util.Set;

public class ScheduleDetailServiceImpl implements ScheduleDetailService {
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

