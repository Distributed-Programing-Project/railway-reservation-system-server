package vn.edu.iuh.fit.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import vn.edu.iuh.fit.common.dto.CustomerDTO;
import vn.edu.iuh.fit.server.model.Customer;

import java.util.List;

@Mapper
public interface CustomerMapper {
    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    @Mapping(target = "fullName", source = "name")
    @Mapping(target = "phone", source = "phoneNumber")
    @Mapping(target = "customerId", source = "id")
    @Mapping(target = "isActive", source = "active")
    CustomerDTO toDto(Customer customer);

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "phoneNumber", source = "phone")
    @Mapping(target = "id", source = "customerId")
    @Mapping(target = "tickets", ignore = true)
    @Mapping(target = "passport", ignore = true)
    Customer toEntity(CustomerDTO dto);

    List<CustomerDTO> toDtoList(List<Customer> customers);
}
