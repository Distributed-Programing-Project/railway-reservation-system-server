package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.dto.StationDTO;
import vn.edu.iuh.fit.common.message.StationMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.repository.StationRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.StationRepositoryImpl;
import vn.edu.iuh.fit.server.service.StationService;

import java.util.List;

public class StationServiceImpl implements StationService {

    private static final Logger log = LoggerFactory.getLogger(StationServiceImpl.class);

    private final StationRepository repository = new StationRepositoryImpl();

    @Override
    public Response getAllStations() {
        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                List<Station> stations = repository.findAllStations(em);
                List<StationDTO> dtos = stations.stream()
                        .map(s -> StationDTO.builder()
                                .id(s.getId())
                                .name(s.getName())
                                .destinationKm(s.getDestinationKm())
                                .build())
                        .toList();
                return Response.success(StationMessages.FIND_ALL_SUCCESS, dtos);
            });
        } catch (Exception e) {
            log.error("Failed to get all stations", e);
            return Response.error(StationMessages.FIND_ALL_FAILED + e.getMessage());
        }
    }

    @Override
    public List<Station> findAllStations() {
        return AbstractGenericRepositoryImpl.readOnly(em -> repository.findAllStations(em));
    }

    @Override
    public List<String> findAllStationNames() {
        return AbstractGenericRepositoryImpl.readOnly(em -> repository.findAllStationNames(em));
    }

    @Override
    public Station findStationByName(String stationName) {
        return AbstractGenericRepositoryImpl.readOnly(em -> repository.findStationByName(em, stationName));
    }

    @Override
    public Station findStationById(String stationId) {
        return AbstractGenericRepositoryImpl.readOnly(em -> repository.findStationById(em, stationId));
    }
}
