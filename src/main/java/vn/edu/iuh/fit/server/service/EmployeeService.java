package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.dto.EmployeeDTO;
import vn.edu.iuh.fit.server.dto.EmployeeFilterDTO;

public interface EmployeeService {

    Response createEmployee(EmployeeDTO employeeDTO);

    Response createEmployeeAccount(String employeeId);

    Response softDeleteEmployee(String employeeId);

    Response resetEmployeePassword(String employeeId);

    Response findAllEmployees(EmployeeFilterDTO filterDTO);
}