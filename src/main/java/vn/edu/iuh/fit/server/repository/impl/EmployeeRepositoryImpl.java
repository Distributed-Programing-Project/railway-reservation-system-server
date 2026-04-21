package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.constant.EmployeeStatus;
import vn.edu.iuh.fit.server.model.Account;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;

public class EmployeeRepositoryImpl extends AbstractGenericRepositoryImpl<Employee, String>
        implements EmployeeRepository {

    public EmployeeRepositoryImpl() {
        super(Employee.class);
    }

    @Override
    public Employee saveEmployee(EntityManager em, Employee employee) {
        em.persist(employee);
        return employee;
    }

    @Override
    public Employee findEmployeeById(EntityManager em, String employeeId) {
        return em.find(Employee.class, employeeId);
    }

    @Override
    public List<Employee> findAllEmployees(EntityManager em, int page, int size, EmployeeStatus statusFilter) {
        String jpql = statusFilter == null
                ? "SELECT e FROM Employee e LEFT JOIN FETCH e.account ORDER BY e.createdAt DESC"
                : "SELECT e FROM Employee e LEFT JOIN FETCH e.account WHERE e.employeeStatus = :status ORDER BY e.createdAt DESC";
        var query = em.createQuery(jpql, Employee.class);
        if (statusFilter != null) {
            query.setParameter("status", statusFilter);
        }
        return query.setFirstResult(page * size).setMaxResults(size).getResultList();
    }

    @Override
    public long countEmployees(EntityManager em, EmployeeStatus statusFilter) {
        String jpql = statusFilter == null
                ? "SELECT COUNT(e) FROM Employee e"
                : "SELECT COUNT(e) FROM Employee e WHERE e.employeeStatus = :status";
        var query = em.createQuery(jpql, Long.class);
        if (statusFilter != null) {
            query.setParameter("status", statusFilter);
        }
        return query.getSingleResult();
    }

    @Override
    public boolean existsByNationalId(EntityManager em, String nationalId) {
        return em.createQuery("SELECT COUNT(e) FROM Employee e WHERE e.nationalId = :nationalId", Long.class)
                .setParameter("nationalId", nationalId)
                .getSingleResult() > 0;
    }

    @Override
    public boolean existsByEmail(EntityManager em, String email) {
        return em.createQuery("SELECT COUNT(e) FROM Employee e WHERE e.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult() > 0;
    }

    @Override
    public String generateEmployeeCode(EntityManager em, Boolean isManager) {
        String prefix = Boolean.TRUE.equals(isManager) ? "QL" : "NV";
        String lastCode = em.createQuery(
                        "SELECT e.employeeCode FROM Employee e WHERE e.employeeCode LIKE :prefix ORDER BY LENGTH(e.employeeCode) DESC, e.employeeCode DESC",
                        String.class)
                .setParameter("prefix", prefix + "%")
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
        int nextNum = lastCode == null ? 1 : Integer.parseInt(lastCode.substring(prefix.length())) + 1;
        return String.format("%s%03d", prefix, nextNum);
    }

    @Override
    public String createAndLinkAccount(EntityManager em, String employeeId, String username, String hashedPassword) {
        Account account = Account.builder()
                .username(username)
                .password(hashedPassword)
                .active(true)
                .build();
        em.persist(account);

        Employee employee = em.find(Employee.class, employeeId);
        employee.setAccount(account);
        employee.setUpdatedAt(LocalDate.now());
        return account.getUsername();
    }

    @Override
    public Employee softDeleteEmployee(EntityManager em, String employeeId) {
        Employee employee = em.find(Employee.class, employeeId);
        if (employee == null) throw new IllegalArgumentException("Nhân viên không tồn tại: id=" + employeeId);
        employee.setEmployeeStatus(EmployeeStatus.INACTIVE);
        employee.setUpdatedAt(LocalDate.now());
        if (employee.getAccount() != null) {
            employee.getAccount().setActive(false);
        }
        return employee;
    }

    @Override
    public String resetAccountPassword(EntityManager em, String employeeId, String hashedPassword) {
        Employee employee = em.find(Employee.class, employeeId);
        if (employee == null) throw new IllegalArgumentException("Nhân viên không tồn tại: id=" + employeeId);
        Account account = employee.getAccount();
        account.setPassword(hashedPassword);
        return account.getUsername();
    }
}
