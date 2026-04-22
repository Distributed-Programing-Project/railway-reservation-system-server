package vn.edu.iuh.fit.server.mapper;

import vn.edu.iuh.fit.common.dto.EmployeeDTO;
import vn.edu.iuh.fit.server.model.Employee;

import java.util.List;

public class EmployeeMapper {

    private static final GenericDataMapper mapper = new JacksonDataMapper();

    public static EmployeeDTO toDto(Employee employee) {
        if (employee == null) return null;
        EmployeeDTO dto = mapper.toObject(mapper.toMap(employee), EmployeeDTO.class);
        if (employee.getAccount() != null) {
            dto.setAccountId(employee.getAccount().getId());
        }
        return dto;
    }

    public static Employee toEntity(EmployeeDTO employeeDTO) {
        if (employeeDTO == null) return null;
        return mapper.toObject(mapper.toMap(employeeDTO), Employee.class);
    }

    public static List<EmployeeDTO> toDtoList(List<Employee> employees) {
        return employees.stream()
                .map(EmployeeMapper::toDto)
                .toList();
    }
}
