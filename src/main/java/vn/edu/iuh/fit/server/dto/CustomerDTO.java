package vn.edu.iuh.fit.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String customerId;

  @NotBlank(message = "Họ tên không được để trống")
  private String fullName;

  @NotBlank(message = "CCCD không được để trống")
  @Pattern(regexp = "^[0-9]{9,12}$", message = "CCCD phải gồm 9-12 chữ số")
  private String idCard;

  @Pattern(regexp = "^(\\d{10})?$", message = "Số điện thoại phải gồm đúng 10 chữ số")
  private String phone;

  @Email(message = "Email không đúng định dạng")
  private String email;

  private Boolean isActive;
}
