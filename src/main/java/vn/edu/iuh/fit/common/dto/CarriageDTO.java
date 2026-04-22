package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.CarriageType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarriageDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String id;
  private int number;
  private CarriageType type;
  private String trainId;
}
