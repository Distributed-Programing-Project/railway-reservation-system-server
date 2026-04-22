package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.server.model.Employee;

import java.util.List;

public interface EmployeeRepository {

    Employee saveEmployee(EntityManager em, Employee employee);
    Employee findEmployeeById(EntityManager em, String employeeId);
    List<Employee> findAllEmployees(EntityManager em, int page, int size, EmployeeStatus statusFilter);
    long countEmployees(EntityManager em, EmployeeStatus statusFilter);
    boolean existsByNationalId(EntityManager em, String nationalId);
    boolean existsByEmail(EntityManager em, String email);
    String generateEmployeeCode(EntityManager em, Boolean isManager);
    String createAndLinkAccount(EntityManager em, String employeeId, String username, String hashedPassword);
    Employee softDeleteEmployee(EntityManager em, String employeeId);
    String resetAccountPassword(EntityManager em, String employeeId, String hashedPassword);
}
