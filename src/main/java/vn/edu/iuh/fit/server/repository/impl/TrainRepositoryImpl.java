package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.constant.TrainStatus;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.TrainRepository;

import java.util.List;

public class TrainRepositoryImpl extends AbstractGenericRepositoryImpl<Train, String>
        implements TrainRepository {

    public TrainRepositoryImpl() {
        super(Train.class);
    }

    @Override
    public List<Train> findAllTrains(TrainStatus statusFilter) {
        return doWithEntityManager(em -> {
            if (statusFilter != null) {
                return em.createQuery(
                        "SELECT DISTINCT t FROM Train t LEFT JOIN FETCH t.carriages WHERE t.status = :status ORDER BY t.trainCode",
                        Train.class)
                        .setParameter("status", statusFilter)
                        .getResultList();
            }
            return em.createQuery(
                    "SELECT DISTINCT t FROM Train t LEFT JOIN FETCH t.carriages ORDER BY t.trainCode",
                    Train.class)
                    .getResultList();
        });
    }

    @Override
    public List<Train> findTrainsByCodeLike(String keyword) {
        return doWithEntityManager(em ->
                em.createQuery(
                        "SELECT DISTINCT t FROM Train t LEFT JOIN FETCH t.carriages WHERE LOWER(t.trainCode) LIKE LOWER(:keyword) ORDER BY t.trainCode",
                        Train.class)
                        .setParameter("keyword", "%" + keyword + "%")
                        .getResultList()
        );
    }

    @Override
    public boolean existsByTrainCodeIgnoreCase(String trainCode) {
        return doWithEntityManager(em ->
                em.createQuery(
                        "SELECT COUNT(t) FROM Train t WHERE LOWER(t.trainCode) = LOWER(:code)",
                        Long.class)
                        .setParameter("code", trainCode)
                        .getSingleResult() > 0
        );
    }

    @Override
    public Train createTrain(EntityManager em, String trainCode, List<String> orderedCarriageIds) {
        Train train = Train.builder()
                .trainCode(trainCode)
                .status(TrainStatus.ACTIVE)
                .build();
        em.persist(train);

        for (int i = 0; i < orderedCarriageIds.size(); i++) {
            Carriage carriage = em.find(Carriage.class, orderedCarriageIds.get(i));
            if (carriage == null || carriage.getTrain() != null) {
                throw new IllegalStateException(
                        "Toa không hợp lệ hoặc đã được gán cho tàu khác: " + orderedCarriageIds.get(i));
            }
            carriage.setTrain(train);
            carriage.setNumber(i + 1);
        }
        return train;
    }

    @Override
    public void updateTrainCarriages(EntityManager em, String trainId, List<String> orderedCarriageIds) {
        Train train = em.find(Train.class, trainId);
        if (train == null) throw new IllegalArgumentException("Không tìm thấy tàu: " + trainId);

        List<Carriage> currentCarriages = em.createQuery(
                "SELECT c FROM Carriage c WHERE c.train.id = :trainId", Carriage.class)
                .setParameter("trainId", trainId)
                .getResultList();
        for (Carriage carriage : currentCarriages) {
            carriage.setTrain(null);
            carriage.setNumber(0);
        }
        em.flush();

        for (int i = 0; i < orderedCarriageIds.size(); i++) {
            Carriage carriage = em.find(Carriage.class, orderedCarriageIds.get(i));
            if (carriage == null || carriage.getTrain() != null) {
                throw new IllegalStateException(
                        "Toa không hợp lệ hoặc đã được gán cho tàu khác: " + orderedCarriageIds.get(i));
            }
            carriage.setTrain(train);
            carriage.setNumber(i + 1);
        }
    }

    @Override
    public void updateTrainStatus(EntityManager em, String trainId, TrainStatus status) {
        Train train = em.find(Train.class, trainId);
        if (train == null) throw new IllegalArgumentException("Không tìm thấy tàu: " + trainId);
        train.setStatus(status);
    }
}
