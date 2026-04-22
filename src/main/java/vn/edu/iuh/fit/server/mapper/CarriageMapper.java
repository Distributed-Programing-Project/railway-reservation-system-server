package vn.edu.iuh.fit.server.mapper;

import vn.edu.iuh.fit.common.dto.CarriageDTO;
import vn.edu.iuh.fit.server.model.Carriage;

import java.util.List;

public class CarriageMapper {

    public static CarriageDTO toDto(Carriage carriage) {
        if (carriage == null) return null;
        return CarriageDTO.builder()
                .id(carriage.getId())
                .number(carriage.getNumber())
                .type(carriage.getType())
                .trainId(carriage.getTrain() != null ? carriage.getTrain().getId() : null)
                .build();
    }

    public static List<CarriageDTO> toDtoList(List<Carriage> carriages) {
        if (carriages == null) return List.of();
        return carriages.stream().map(CarriageMapper::toDto).toList();
    }
}
