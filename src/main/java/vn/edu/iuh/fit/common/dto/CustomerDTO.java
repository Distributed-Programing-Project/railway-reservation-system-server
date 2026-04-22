package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO implements Serializable {
  private String id;
  private String name;
  private String idCard;
  private String passport;
  private String phoneNumber;
  private String email;
}
