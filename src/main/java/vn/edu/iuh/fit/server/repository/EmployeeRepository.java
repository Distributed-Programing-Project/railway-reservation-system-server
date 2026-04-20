package vn.edu.iuh.fit.server.repository;

import vn.edu.iuh.fit.server.constant.EmployeeStatus;
import vn.edu.iuh.fit.server.model.Employee;

import java.util.List;

public interface EmployeeRepository {

    Employee saveEmployee(Employee employee);

    Employee findEmployeeById(String employeeId);

    List<Employee> findAllEmployees(int page, int size, EmployeeStatus statusFilter);

    long countEmployees(EmployeeStatus statusFilter);

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    String generateEmployeeCode(Boolean isManager);

    String createAndLinkAccount(String employeeId, String username, String hashedPassword);

    Employee softDeleteEmployee(String employeeId);

    String resetAccountPassword(String employeeId, String hashedPassword);
}
