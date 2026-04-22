package vn.edu.iuh.fit.server.mapper;

import vn.edu.iuh.fit.server.constant.CarriageType;
import vn.edu.iuh.fit.server.dto.CarriageDTO;
import vn.edu.iuh.fit.server.dto.TrainDTO;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Train;

import java.util.List;
import java.util.Map;

public class TrainMapper {

    private static final Map<CarriageType, Integer> SEAT_COUNT_BY_TYPE = Map.of(
            CarriageType.HARD_SEAT, 64,
            CarriageType.SOFT_SEAT, 56,
            CarriageType.SOFT_SEAT_AC, 56,
            CarriageType.BERTH_6, 42,
            CarriageType.BERTH_4, 36
    );

    public static TrainDTO toDto(Train train) {
        if (train == null) return null;
        List<Carriage> carriages = train.getCarriages() != null ? train.getCarriages() : List.of();
        int totalSeats = carriages.stream()
                .mapToInt(carriage -> SEAT_COUNT_BY_TYPE.getOrDefault(carriage.getType(), 0))
                .sum();
        return TrainDTO.builder()
                .id(train.getId())
                .trainCode(train.getTrainCode())
                .status(train.getStatus())
                .totalCarriages(carriages.size())
                .totalSeats(totalSeats)
                .carriages(null)
                .build();
    }

    public static TrainDTO toDtoWithCarriages(Train train) {
        if (train == null) return null;
        TrainDTO trainDTO = toDto(train);
        List<CarriageDTO> carriageDTOs = CarriageMapper.toDtoList(train.getCarriages());
        trainDTO.setCarriages(carriageDTOs);
        return trainDTO;
    }

    public static List<TrainDTO> toDtoList(List<Train> trains) {
        return trains.stream().map(TrainMapper::toDto).toList();
    }
}
