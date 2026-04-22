package vn.edu.iuh.fit.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import vn.edu.iuh.fit.common.dto.CarriageDTO;
import vn.edu.iuh.fit.server.model.Carriage;

import java.util.List;

@Mapper
public interface CarriageMapper {
    CarriageMapper INSTANCE = Mappers.getMapper(CarriageMapper.class);

    @Mapping(source = "train.id", target = "trainId")
    CarriageDTO toDto(Carriage carriage);

    List<CarriageDTO> toDtoList(List<Carriage> carriages);
}
