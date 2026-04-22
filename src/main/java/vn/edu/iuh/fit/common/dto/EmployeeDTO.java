package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import vn.edu.iuh.fit.common.message.EmployeeMessages;

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

    @NotBlank(message = EmployeeMessages.NAME_REQUIRED)
    private String employeeName;

    @NotBlank(message = EmployeeMessages.NATIONAL_ID_REQUIRED)
    @Pattern(regexp = "\\d{9}|\\d{12}", message = EmployeeMessages.NATIONAL_ID_FORMAT)
    private String nationalId;

    @NotNull(message = EmployeeMessages.DATE_OF_BIRTH_REQUIRED)
    private LocalDate dateOfBirth;

    private Boolean gender;
    private String address;

    @NotBlank(message = EmployeeMessages.PHONE_REQUIRED)
    @Pattern(regexp = "\\d{10}", message = EmployeeMessages.PHONE_FORMAT)
    private String phoneNumber;

    @NotBlank(message = EmployeeMessages.EMAIL_REQUIRED)
    @Email(message = EmployeeMessages.EMAIL_FORMAT)
    private String email;

    @NotNull(message = EmployeeMessages.IS_MANAGER_REQUIRED)
    private Boolean isManager;

    private String employeeStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String accountId;
}
