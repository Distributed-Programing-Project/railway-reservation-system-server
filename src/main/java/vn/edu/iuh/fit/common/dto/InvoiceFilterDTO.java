package vn.edu.iuh.fit.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.common.message.InvoiceMessages;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceFilterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = InvoiceMessages.REQUEST_EMPLOYEE_ID_REQUIRED)
    private String requestEmployeeId;

    private String keyword;

    @Min(value = 1, message = InvoiceMessages.DAY_INVALID)
    @Max(value = 31, message = InvoiceMessages.DAY_INVALID)
    private Integer day;

    @Min(value = 1, message = InvoiceMessages.MONTH_INVALID)
    @Max(value = 12, message = InvoiceMessages.MONTH_INVALID)
    private Integer month;

    @Min(value = 2000, message = InvoiceMessages.YEAR_INVALID)
    private Integer year;

    private InvoiceType type;

    private String filterEmployeeId;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
