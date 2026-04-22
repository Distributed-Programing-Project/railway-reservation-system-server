package vn.edu.iuh.fit.common.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<EmployeeDTO> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
