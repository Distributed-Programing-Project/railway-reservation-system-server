package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSearchDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String keyword;

  @Builder.Default
  @Min(value = 0, message = "Page không được âm")
  private int page = 0;

  @Builder.Default
  @Min(value = 1, message = "Page size phải lớn hơn 0")
  private int size = 20;
}
