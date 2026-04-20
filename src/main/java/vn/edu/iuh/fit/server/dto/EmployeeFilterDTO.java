package vn.edu.iuh.fit.server.dto;

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
    private int page = 0;

    @Builder.Default
    private int size = 20;

    private EmployeeStatus statusFilter;
}
