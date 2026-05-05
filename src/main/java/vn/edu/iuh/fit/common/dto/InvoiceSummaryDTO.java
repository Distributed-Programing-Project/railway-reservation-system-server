package vn.edu.iuh.fit.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.InvoiceType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceSummaryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issueDate;

    private double totalAmount;
    private InvoiceType type;
    private String customerName;
    private String employeeName;
    private int ticketCount;
}
