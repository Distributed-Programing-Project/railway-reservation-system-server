package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.TicketCategory;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleScheduleSearchDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String departureStationId;
  private String destinationStationId;
  private LocalDate departureDate;
  private TicketCategory ticketCategory;
  private LocalDate returnDate;

  @Builder.Default
  private int page = 0;

  @Builder.Default
  private int size = 20;
}

