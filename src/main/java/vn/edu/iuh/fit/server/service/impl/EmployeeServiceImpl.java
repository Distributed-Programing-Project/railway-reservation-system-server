package vn.edu.iuh.fit.server.service.impl;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.constant.EmployeeStatus;
import vn.edu.iuh.fit.server.dto.AccountCreatedDTO;
import vn.edu.iuh.fit.server.dto.EmployeeDTO;
import vn.edu.iuh.fit.server.dto.EmployeeFilterDTO;
import vn.edu.iuh.fit.server.dto.EmployeePageDTO;
import vn.edu.iuh.fit.server.mapper.EmployeeMapper;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.service.EmployeeService;
import vn.edu.iuh.fit.server.util.ValidationUtils;

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

        try {
            if (repository.existsByNationalId(employeeDTO.getNationalId())) {
                return Response.error("CCCD đã được đăng ký cho nhân viên khác");
            }
            if (repository.existsByEmail(employeeDTO.getEmail())) {
                return Response.error("Email đã được sử dụng bởi tài khoản khác");
            }

            String employeeCode = repository.generateEmployeeCode(employeeDTO.getIsManager());

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

            Employee savedEmployee = repository.saveEmployee(employee);
            log.info("Employee created: employeeCode={}, id={}", savedEmployee.getEmployeeCode(), savedEmployee.getEmployeeId());
            return Response.success("Tạo nhân viên thành công", EmployeeMapper.toDto(savedEmployee));
        } catch (Exception e) {
            log.error("Failed to create employee: nationalId={}", employeeDTO.getNationalId(), e);
            return Response.error("Lỗi hệ thống, vui lòng thử lại");
        }
    }

    @Override
    public Response createEmployeeAccount(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return Response.error("ID nhân viên không được để trống");
        }

        try {
            Employee employee = repository.findEmployeeById(employeeId);
            if (employee == null) {
                return Response.error("Không tìm thấy nhân viên: id=" + employeeId);
            }
            if (employee.getAccount() != null) {
                return Response.error("Nhân viên đã được cấp tài khoản");
            }

            String rawPassword = generateRawPassword();
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            String username = repository.createAndLinkAccount(employeeId, employee.getEmployeeCode(), hashedPassword);
            log.info("Account created for employee: employeeId={}, username={}", employeeId, username);
            return buildAccountCreatedResponse("Cấp tài khoản thành công", username, rawPassword);
        } catch (Exception e) {
            log.error("Failed to create account for employee: employeeId={}", employeeId, e);
            return Response.error("Lỗi hệ thống, vui lòng thử lại");
        }
    }

    @Override
    public Response softDeleteEmployee(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return Response.error("ID nhân viên không được để trống");
        }

        try {
            Employee employee = repository.findEmployeeById(employeeId);
            if (employee == null) {
                return Response.error("Không tìm thấy nhân viên: id=" + employeeId);
            }
            if (employee.getEmployeeStatus() == EmployeeStatus.INACTIVE) {
                return Response.error("Nhân viên này đã bị xoá khỏi hệ thống");
            }

            Employee updatedEmployee = repository.softDeleteEmployee(employeeId);
            log.info("Employee soft-deleted: employeeId={}", employeeId);
            return Response.success("Xoá mềm nhân viên thành công", EmployeeMapper.toDto(updatedEmployee));
        } catch (Exception e) {
            log.error("Failed to soft-delete employee: employeeId={}", employeeId, e);
            return Response.error("Lỗi hệ thống, vui lòng thử lại");
        }
    }

    @Override
    public Response resetEmployeePassword(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return Response.error("ID nhân viên không được để trống");
        }

        try {
            Employee employee = repository.findEmployeeById(employeeId);
            if (employee == null) {
                return Response.error("Không tìm thấy nhân viên: id=" + employeeId);
            }
            if (employee.getAccount() == null) {
                return Response.error("Nhân viên chưa được cấp tài khoản");
            }

            String rawPassword = generateRawPassword();
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            String username = repository.resetAccountPassword(employeeId, hashedPassword);
            log.info("Password reset for employee: employeeId={}", employeeId);
            return buildAccountCreatedResponse("Reset mật khẩu thành công", username, rawPassword);
        } catch (Exception e) {
            log.error("Failed to reset password for employee: employeeId={}", employeeId, e);
            return Response.error("Lỗi hệ thống, vui lòng thử lại");
        }
    }

    @Override
    public Response findAllEmployees(EmployeeFilterDTO filterDTO) {
        try {
            EmployeeFilterDTO filter = filterDTO != null ? filterDTO : new EmployeeFilterDTO();
            int page = filter.getPage();
            int size = filter.getSize();
            EmployeeStatus statusFilter = filter.getStatusFilter();

            List<Employee> employees = repository.findAllEmployees(page, size, statusFilter);
            long totalElements = repository.countEmployees(statusFilter);
            int totalPages = (int) Math.ceil((double) totalElements / size);

            EmployeePageDTO pageDTO = EmployeePageDTO.builder()
                    .content(EmployeeMapper.toDtoList(employees))
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .currentPage(page)
                    .build();
            return Response.success("Lấy danh sách nhân viên thành công", pageDTO);
        } catch (Exception e) {
            log.error("Failed to find all employees", e);
            return Response.error("Lỗi hệ thống, vui lòng thử lại");
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

