package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerHistoryRequestDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @NotBlank(message = "Customer ID không được để trống")
  private String customerId;
}

