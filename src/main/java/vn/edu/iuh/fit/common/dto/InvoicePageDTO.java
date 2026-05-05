package vn.edu.iuh.fit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoicePageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<InvoiceSummaryDTO> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;
}
