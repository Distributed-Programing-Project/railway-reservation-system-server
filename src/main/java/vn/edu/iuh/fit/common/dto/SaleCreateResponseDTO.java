package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleCreateResponseDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String invoiceId;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime invoiceDate;

  private double totalAmount;
  private double amountPaid;
  private double changeAmount;

  private int earnedPoints;
  private int redeemedPoints;

  private List<IssuedTicketDTO> tickets;
  private List<IssuedTicketDTO> childVouchers;
}

