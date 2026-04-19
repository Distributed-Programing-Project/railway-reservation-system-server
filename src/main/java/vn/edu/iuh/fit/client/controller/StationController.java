package vn.edu.iuh.fit.client.controller;

import vn.edu.iuh.fit.server.model.Station;

import java.util.List;

public interface StationController {
    List<Station> findAllStations();

    List<String> findAllStationNames();

    Station findStationByName(String stationName);

    Station findStationById(String stationId);
}

