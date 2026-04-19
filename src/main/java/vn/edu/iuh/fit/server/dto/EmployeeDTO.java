package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO implements Serializable {

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