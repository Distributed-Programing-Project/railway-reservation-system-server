package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.message.TicketMessages;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeEligibleTicketSearchDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  @NotBlank(message = TicketMessages.ID_CARD_REQUIRED)
  private String idCard;
}

