package vn.edu.iuh.fit.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.server.util.id.annotation.GeneratedEmployeeId;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"account", "invoiceList"})
@Builder
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedEmployeeId
    @Column(name = "employee_id", length = 12)
    private String employeeId;

    @Column(name = "employee_code", unique = true, nullable = false, length = 10)
    private String employeeCode;

    @Column(name = "employee_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String employeeName;

    @Column(name = "national_id", unique = true, nullable = false, length = 12)
    private String nationalId;

    @Column(name = "address", columnDefinition = "NVARCHAR(255)")
    private String address;

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

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @JsonIgnore
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
    private List<Invoice> invoiceList;
}
