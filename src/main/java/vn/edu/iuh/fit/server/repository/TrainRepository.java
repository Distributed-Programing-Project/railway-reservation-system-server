package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.TrainStatus;
import vn.edu.iuh.fit.server.model.Train;

import java.util.List;

public interface TrainRepository {
    List<Train> findAllTrains(TrainStatus statusFilter);
    List<Train> findTrainsByCodeLike(String keyword);
    boolean existsByTrainCodeIgnoreCase(String trainCode);
    Train createTrain(EntityManager em, String trainCode, List<String> orderedCarriageIds);
    void updateTrainCarriages(EntityManager em, String trainId, List<String> orderedCarriageIds);
    void updateTrainStatus(EntityManager em, String trainId, TrainStatus status);
}
