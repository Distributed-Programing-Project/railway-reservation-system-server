package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
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

  @jakarta.validation.constraints.NotBlank(message = "Họ tên không được để trống")
  private String fullName;

  @Pattern(regexp = "^[0-9]{9,12}$", message = "CCCD phải gồm 9-12 chữ số")
  private String idCard;

  @Pattern(regexp = "^[A-Za-z0-9]{1,9}$", message = "Hộ chiếu tối đa 9 ký tự, chỉ gồm chữ và số")
  private String passport;

  @Pattern(regexp = "^(\\d{10})?$", message = "Số điện thoại phải gồm đúng 10 chữ số")
  private String phone;

  @Email(message = "Email không đúng định dạng")
  private String email;

  private Boolean isActive;

  private int rewardPoints;

  @AssertTrue(message = "Cần cung cấp CCCD/CMND hoặc hộ chiếu")
  public boolean isDocumentPresent() {
    boolean hasIdCard = idCard != null && !idCard.isBlank();
    boolean hasPassport = passport != null && !passport.isBlank();
    return hasIdCard || hasPassport;
  }
}
