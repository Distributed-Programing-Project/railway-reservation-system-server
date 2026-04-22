package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.Min;
import lombok.*;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.message.EmployeeMessages;

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
    @Min(value = 0, message = EmployeeMessages.PAGE_NOT_NEGATIVE)
    private int page = 0;

    @Builder.Default
    @Min(value = 1, message = EmployeeMessages.PAGE_SIZE_MIN)
    private int size = 20;

    private EmployeeStatus statusFilter;
}
