package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.common.enums.EmployeeStatus;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString()
@Builder
public class Employee {
    @Id
    @Column(name = "employee_id", length = 20)
    private String employeeId;

    @Column(name = "employee_name", nullable = false, length = 100)
    private String employeeName;

    @Column(name = "national_id", unique = true, nullable = false, length = 11)
    private String nationalId; // CCCD

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private Boolean gender; // true = Male, false = Female

    @Column(name = "phone_number", length = 10)
    private String phoneNumber;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "is_manager", nullable = false)
    private Boolean isManager; // true = Manager, false = Employee

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 50)
    private EmployeeStatus employeeStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @OneToOne
    @JoinColumn(name = "username", referencedColumnName = "username", unique = true)
    private Account account;

    @OneToMany(mappedBy = "employee")
    private List<Invoice> invoiceList;
}