package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.model.Station;

import java.util.List;

public interface StationService {
    Response getAllStations();

    List<Station> findAllStations();

    List<String> findAllStationNames();

    Station findStationByName(String stationName);

    Station findStationById(String stationId);
}

