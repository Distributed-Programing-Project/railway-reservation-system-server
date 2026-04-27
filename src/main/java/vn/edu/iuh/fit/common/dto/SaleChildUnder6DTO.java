package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.TripDirection;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleChildUnder6DTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String childName;
  private LocalDate dateOfBirth;
  private TripDirection accompanyDirection;
  private int accompanyPassengerIndex;
}

