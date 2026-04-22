package vn.edu.iuh.fit.server.service.impl;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.dto.AccountCreatedDTO;
import vn.edu.iuh.fit.common.dto.EmployeeDTO;
import vn.edu.iuh.fit.common.dto.EmployeeFilterDTO;
import vn.edu.iuh.fit.common.dto.EmployeePageDTO;
import vn.edu.iuh.fit.server.mapper.EmployeeMapper;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.service.EmployeeService;
import vn.edu.iuh.fit.server.util.JPAUtils;
import vn.edu.iuh.fit.server.util.ValidationUtils;
import vn.edu.iuh.fit.common.message.EmployeeMessages;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);
    private final EmployeeRepository repository = new EmployeeRepositoryImpl();

    @Override
    public Response createEmployee(EmployeeDTO employeeDTO) {
        List<String> errors = ValidationUtils.validate(employeeDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        EntityManager em = JPAUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (repository.existsByNationalId(em, employeeDTO.getNationalId())) {
                return Response.error(EmployeeMessages.NATIONAL_ID_ALREADY_EXISTS);
            }
            if (repository.existsByEmail(em, employeeDTO.getEmail())) {
                return Response.error(EmployeeMessages.EMAIL_ALREADY_EXISTS);
            }


            String employeeCode = repository.generateEmployeeCode(em, employeeDTO.getIsManager());

            Employee employee = Employee.builder()
                    .employeeCode(employeeCode)
                    .employeeName(employeeDTO.getEmployeeName())
                    .nationalId(employeeDTO.getNationalId())
                    .dateOfBirth(employeeDTO.getDateOfBirth())
                    .gender(employeeDTO.getGender())
                    .address(employeeDTO.getAddress())
                    .phoneNumber(employeeDTO.getPhoneNumber())
                    .email(employeeDTO.getEmail())
                    .isManager(employeeDTO.getIsManager())
                    .employeeStatus(EmployeeStatus.ACTIVE)
                    .createdAt(LocalDate.now())
                    .build();

            Employee savedEmployee = repository.saveEmployee(em, employee);
            tx.commit();
            log.info("Employee created: employeeCode={}, id={}", savedEmployee.getEmployeeCode(), savedEmployee.getEmployeeId());
            return Response.success(EmployeeMessages.CREATE_SUCCESS, EmployeeMapper.toDto(savedEmployee));

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to create employee: nationalId={}", employeeDTO.getNationalId(), e);
            return Response.error(EmployeeMessages.SYSTEM_ERROR_PREFIX + e.getMessage());

        } finally {
            if (tx.isActive()) tx.rollback();
            em.close();
        }
    }

    @Override
    public Response createEmployeeAccount(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return Response.error(EmployeeMessages.EMPLOYEE_ID_REQUIRED);
        }


        EntityManager em = JPAUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Employee employee = repository.findEmployeeById(em, employeeId);
            if (employee == null) {
                return Response.error(EmployeeMessages.notFoundById(employeeId));
            }
            if (employee.getAccount() != null) {
                return Response.error(EmployeeMessages.ACCOUNT_ALREADY_EXISTS);
            }


            String rawPassword = generateRawPassword();
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            String username = repository.createAndLinkAccount(em, employeeId, employee.getEmployeeCode(), hashedPassword);
            tx.commit();
            log.info("Account created for employee: employeeId={}, username={}", employeeId, username);
            return buildAccountCreatedResponse(EmployeeMessages.ACCOUNT_CREATE_SUCCESS, username, rawPassword);

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to create account for employee: employeeId={}", employeeId, e);
            return Response.error(EmployeeMessages.SYSTEM_ERROR_PREFIX + e.getMessage());

        } finally {
            if (tx.isActive()) tx.rollback();
            em.close();
        }
    }

    @Override
    public Response softDeleteEmployee(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return Response.error(EmployeeMessages.EMPLOYEE_ID_REQUIRED);
        }


        EntityManager em = JPAUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Employee employee = repository.findEmployeeById(em, employeeId);
            if (employee == null) {
                return Response.error(EmployeeMessages.notFoundById(employeeId));
            }
            if (employee.getEmployeeStatus() == EmployeeStatus.INACTIVE) {
                return Response.error(EmployeeMessages.ALREADY_INACTIVE);
            }


            Employee updatedEmployee = repository.softDeleteEmployee(em, employeeId);
            tx.commit();
            log.info("Employee soft-deleted: employeeId={}", employeeId);
            return Response.success(EmployeeMessages.SOFT_DELETE_SUCCESS, EmployeeMapper.toDto(updatedEmployee));

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to soft-delete employee: employeeId={}", employeeId, e);
            return Response.error(EmployeeMessages.SYSTEM_ERROR_PREFIX + e.getMessage());

        } finally {
            if (tx.isActive()) tx.rollback();
            em.close();
        }
    }

    @Override
    public Response resetEmployeePassword(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return Response.error(EmployeeMessages.EMPLOYEE_ID_REQUIRED);
        }


        EntityManager em = JPAUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Employee employee = repository.findEmployeeById(em, employeeId);
            if (employee == null) {
                return Response.error(EmployeeMessages.notFoundById(employeeId));
            }
            if (employee.getAccount() == null) {
                return Response.error(EmployeeMessages.ACCOUNT_NOT_EXISTS);
            }


            String rawPassword = generateRawPassword();
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            String username = repository.resetAccountPassword(em, employeeId, hashedPassword);
            tx.commit();
            log.info("Password reset for employee: employeeId={}", employeeId);
            return buildAccountCreatedResponse(EmployeeMessages.PASSWORD_RESET_SUCCESS, username, rawPassword);

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to reset password for employee: employeeId={}", employeeId, e);
            return Response.error(EmployeeMessages.SYSTEM_ERROR_PREFIX + e.getMessage());

        } finally {
            if (tx.isActive()) tx.rollback();
            em.close();
        }
    }

    @Override
    public Response findAllEmployees(EmployeeFilterDTO filterDTO) {
        EntityManager em = JPAUtils.getEntityManager();
        try {
            EmployeeFilterDTO filter = filterDTO != null ? filterDTO : new EmployeeFilterDTO();
            int page = filter.getPage();
            int size = filter.getSize();
            EmployeeStatus statusFilter = filter.getStatusFilter();

            List<Employee> employees = repository.findAllEmployees(em, page, size, statusFilter);
            long totalElements = repository.countEmployees(em, statusFilter);
            int totalPages = (int) Math.ceil((double) totalElements / size);

            EmployeePageDTO pageDTO = EmployeePageDTO.builder()
                    .content(EmployeeMapper.toDtoList(employees))
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .currentPage(page)
                    .build();
            return Response.success(EmployeeMessages.FIND_ALL_SUCCESS, pageDTO);

        } catch (Exception e) {
            log.error("Failed to find all employees", e);
            return Response.error(EmployeeMessages.SYSTEM_ERROR_PREFIX + e.getMessage());

        } finally {
            em.close();
        }
    }

    private String generateRawPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private Response buildAccountCreatedResponse(String message, String username, String rawPassword) {
        return Response.success(message, AccountCreatedDTO.builder()
                .username(username)
                .temporaryPassword(rawPassword)
                .build());
    }
}

