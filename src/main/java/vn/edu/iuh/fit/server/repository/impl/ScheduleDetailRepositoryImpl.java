package vn.edu.iuh.fit.server.repository.impl;

import vn.edu.iuh.fit.server.repository.ScheduleDetailRepository;

import java.util.Set;

public class ScheduleDetailRepositoryImpl implements ScheduleDetailRepository {
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

