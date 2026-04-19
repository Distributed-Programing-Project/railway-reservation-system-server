package vn.edu.iuh.fit.server.repository.impl;

import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.repository.StationRepository;

import java.util.List;

public class StationRepositoryImpl implements StationRepository {
    @Override
    public List<Station> findAllStations() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<String> findAllStationNames() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Station findStationByName(String stationName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Station findStationById(String stationId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

