package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.TrainRepository;

import java.util.List;

public class TrainRepositoryImpl extends AbstractGenericRepositoryImpl<Train, String>
        implements TrainRepository {

    public TrainRepositoryImpl() {
        super(Train.class);
    }

    @Override
    public List<Train> findAllTrains(EntityManager em) {
        return em.createQuery("SELECT t FROM Train t", Train.class).getResultList();
    }

    @Override
    public List<Train> getAllTrains(EntityManager em) {
        return findAllTrains(em);
    }

    @Override
    public Train findById(EntityManager em, String trainId) {
        return em.find(Train.class, trainId);
    }

    @Override
    public boolean existsByTrainCode(EntityManager em, String trainCode) {
        return em.createQuery("SELECT COUNT(t) FROM Train t WHERE t.trainCode = :code", Long.class)
                .setParameter("code", trainCode)
                .getSingleResult() > 0;
    }
}
