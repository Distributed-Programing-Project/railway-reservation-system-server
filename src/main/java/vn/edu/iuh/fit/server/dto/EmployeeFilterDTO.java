package vn.edu.iuh.fit.server.dto;

import jakarta.validation.constraints.Min;
import lombok.*;
import vn.edu.iuh.fit.server.constant.EmployeeStatus;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeFilterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    @Min(value = 0, message = "Page không được âm")
    private int page = 0;

    @Builder.Default
    @Min(value = 1, message = "Page size phải lớn hơn 0")
    private int size = 20;

    private EmployeeStatus statusFilter;
}
