package vn.edu.iuh.fit.server.dto;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ExchangeTicketRequestDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  @NotEmpty(message = "Danh sách vé cũ không được để trống")
  private List<String> oldTicketIds;

  @NotEmpty(message = "Danh sách ghế mới không được để trống")
  private List<String> newScheduleDetailIds;

  @Min(value = 0, message = "Số tiền thực nhận không được âm")
  private double cashReceived;

  // Hỗ trợ xuất VAT
  private String taxCode;
  private String companyName;
}
