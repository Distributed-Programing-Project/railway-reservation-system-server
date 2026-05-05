package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.DocumentType;
import vn.edu.iuh.fit.common.constant.TicketType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalePassengerDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String passengerName;
  private DocumentType documentType;
  private String documentNumber;
  private TicketType ticketType;
  private LocalDate dateOfBirth;
  private boolean studentCardVerified;
}