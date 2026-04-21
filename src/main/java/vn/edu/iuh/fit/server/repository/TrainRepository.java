package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Train;

import java.util.List;

public interface TrainRepository {
    List<Train> findAllTrains(EntityManager em);

    List<Train> getAllTrains(EntityManager em);

    Train findById(EntityManager em, String trainId);

    boolean existsByTrainCode(EntityManager em, String trainId);
}

