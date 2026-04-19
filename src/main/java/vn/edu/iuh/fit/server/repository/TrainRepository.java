package vn.edu.iuh.fit.server.repository;

import vn.edu.iuh.fit.server.model.Train;

import java.util.List;

public interface TrainRepository {
    List<Train> findAllTrains();

    List<Train> getAllTrains();

    Train findById(String trainId);

    boolean existsByTrainCode(String trainId);
}

