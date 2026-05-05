package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.CarriageType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarriageSeatMapDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String carriageId;
  private int carriageNumber;
  private CarriageType carriageType;
  private List<SeatMapSeatDTO> seats;
}

