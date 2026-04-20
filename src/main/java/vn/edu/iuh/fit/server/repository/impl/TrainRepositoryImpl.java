package vn.edu.iuh.fit.server.repository.impl;

import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.TrainRepository;

import java.util.List;

public class TrainRepositoryImpl extends AbstractGenericRepositoryImpl<Train, String>
        implements TrainRepository {

    public TrainRepositoryImpl() {
        super(Train.class);
    }

    @Override
    public List<Train> findAllTrains() {
        return doWithEntityManager(em ->
                em.createQuery("SELECT t FROM Train t", Train.class).getResultList());
    }

    @Override
    public List<Train> getAllTrains() {
        return findAllTrains();
    }

    @Override
    public Train findById(String trainId) {
        return doWithEntityManager(em -> em.find(Train.class, trainId));
    }

    @Override
    public boolean existsByTrainCode(String trainCode) {
        return doWithEntityManager(em ->
                em.createQuery("SELECT COUNT(t) FROM Train t WHERE t.trainCode = :code", Long.class)
                        .setParameter("code", trainCode)
                        .getSingleResult() > 0);
    }
}
