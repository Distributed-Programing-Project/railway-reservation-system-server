package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.SeatType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatDTO implements Serializable {
  private String id;
  private int number;
  private SeatType type;
  private String carriageId;
}
