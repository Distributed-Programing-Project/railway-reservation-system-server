package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.enums.CarriageType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarriageDTO implements Serializable {
  private String id;
  private int number;
  private CarriageType type;
  private String trainId;
}
