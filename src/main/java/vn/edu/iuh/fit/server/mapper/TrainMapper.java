package vn.edu.iuh.fit.server.mapper;

import vn.edu.iuh.fit.common.dto.CarriageDTO;
import vn.edu.iuh.fit.common.dto.TrainDTO;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Train;

import java.util.List;

public class TrainMapper {

    public static TrainDTO toDto(Train train) {
        if (train == null) return null;
        List<Carriage> carriages = train.getCarriages() != null ? train.getCarriages() : List.of();
        int totalSeats = carriages.stream()
                .mapToInt(carriage -> carriage.getType().getSeatCount())
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
