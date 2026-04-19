package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.server.constant.InvoiceType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO implements Serializable {
  private String id;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime issueDate;

  private double totalAmount;
  private InvoiceType type;
  private String customerId;
  private String employeeId;

  private List<InvoiceDetailDTO> details;
}
