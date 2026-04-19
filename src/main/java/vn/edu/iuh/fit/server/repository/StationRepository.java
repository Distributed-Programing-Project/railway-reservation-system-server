package vn.edu.iuh.fit.server.repository;

import vn.edu.iuh.fit.server.model.Station;

import java.util.List;

public interface StationRepository {
    List<Station> findAllStations();

    List<String> findAllStationNames();

    Station findStationByName(String stationName);

    Station findStationById(String stationId);
}

