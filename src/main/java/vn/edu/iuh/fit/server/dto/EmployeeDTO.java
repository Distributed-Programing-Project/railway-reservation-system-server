package vn.edu.iuh.fit.server.dto;

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
    private String employeeName;
    private String nationalId;
    private LocalDate dateOfBirth;
    private Boolean gender;
    private String address;
    private String phoneNumber;
    private String email;
    private Boolean isManager;
    private String employeeStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String accountId;
}