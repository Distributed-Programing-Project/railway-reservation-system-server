package vn.edu.iuh.fit.common.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {

    private String employeeId;
    private String employeeName;
    private String nationalId;
    private LocalDate dateOfBirth;
    private Boolean gender;
    private String phoneNumber;
    private String email;
    private Boolean isManager;
    private String employmentStatus;
    private LocalDate createdAt;
    private LocalDate updatedAt;

}