package vn.edu.iuh.fit.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String employeeId;
    private String employeeCode;

    @NotBlank(message = "Họ tên không được để trống")
    private String employeeName;

    @NotBlank(message = "CCCD không được để trống")
    @Pattern(regexp = "\\d{9}|\\d{12}", message = "CCCD phải gồm 9 hoặc 12 chữ số")
    private String nationalId;

    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dateOfBirth;

    private Boolean gender;
    private String address;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "\\d{10}", message = "Số điện thoại phải gồm đúng 10 chữ số")
    private String phoneNumber;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotNull(message = "Loại nhân viên không được để trống")
    private Boolean isManager;

    private String employeeStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String accountId;
}