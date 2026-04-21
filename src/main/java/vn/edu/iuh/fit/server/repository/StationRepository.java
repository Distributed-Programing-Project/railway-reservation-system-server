package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Station;

import java.util.List;

public interface StationRepository {
    List<Station> findAllStations(EntityManager em);

    List<String> findAllStationNames(EntityManager em);

    Station findStationByName(EntityManager em, String stationName);

    Station findStationById(EntityManager em, String stationId);
}

