package vn.edu.iuh.fit.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDeleteRequestDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @NotBlank(message = "Customer ID không được để trống")
  private String customerId;

  @NotBlank(message = "Request employee ID không được để trống")
  private String requestEmployeeId;
}

