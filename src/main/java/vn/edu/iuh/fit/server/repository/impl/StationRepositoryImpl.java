package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.repository.StationRepository;

import java.util.List;

public class StationRepositoryImpl extends AbstractGenericRepositoryImpl<Station, String>
        implements StationRepository {

    public StationRepositoryImpl() {
        super(Station.class);
    }

    @Override
    public List<Station> findAllStations(EntityManager em) {
        return em.createQuery("SELECT s FROM Station s", Station.class).getResultList();
    }

    @Override
    public List<String> findAllStationNames(EntityManager em) {
        return em.createQuery("SELECT s.name FROM Station s", String.class).getResultList();
    }

    @Override
    public Station findStationByName(EntityManager em, String stationName) {
        return em.createQuery("SELECT s FROM Station s WHERE s.name = :name", Station.class)
                .setParameter("name", stationName)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public Station findStationById(EntityManager em, String stationId) {
        return em.find(Station.class, stationId);
    }
}
