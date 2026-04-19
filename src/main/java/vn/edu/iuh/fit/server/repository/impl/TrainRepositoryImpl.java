package vn.edu.iuh.fit.server.repository.impl;

import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.TrainRepository;

import java.util.List;

public class TrainRepositoryImpl implements TrainRepository {
    @Override
    public List<Train> findAllTrains() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Train> getAllTrains() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Train findById(String trainId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsByTrainCode(String trainId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

