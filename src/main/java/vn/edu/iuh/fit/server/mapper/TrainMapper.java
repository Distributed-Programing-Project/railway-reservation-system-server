package vn.edu.iuh.fit.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import vn.edu.iuh.fit.common.dto.TrainDTO;
import vn.edu.iuh.fit.server.model.Train;

import java.util.List;

@Mapper(uses = {CarriageMapper.class})
public interface TrainMapper {
    TrainMapper INSTANCE = Mappers.getMapper(TrainMapper.class);

    @Mapping(target = "totalCarriages", expression = "java(train.getCarriages() != null ? train.getCarriages().size() : 0)")
    @Mapping(target = "totalSeats", expression = "java(countTotalSeats(train))")
    TrainDTO toDto(Train train);

    List<TrainDTO> toDtoList(List<Train> trains);

    default int countTotalSeats(Train train) {
        if (train == null || train.getCarriages() == null) {
            return 0;
        }
        return train.getCarriages().stream()
                .filter(c -> c.getType() != null)
                .mapToInt(c -> c.getType().getSeatCount())
                .sum();
    }
}
