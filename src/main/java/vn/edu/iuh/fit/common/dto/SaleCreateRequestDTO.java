package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.PaymentMethod;
import vn.edu.iuh.fit.common.constant.TicketCategory;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleCreateRequestDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String clientSessionId;

  private TicketCategory ticketCategory;
  private String outboundScheduleId;
  private String returnScheduleId;

  private List<String> outboundScheduleDetailIds;
  private List<String> returnScheduleDetailIds;

  private List<SalePassengerDTO> outboundPassengers;
  private List<SalePassengerDTO> returnPassengers;
  private List<SaleChildUnder6DTO> childrenUnder6;

  private SaleBuyerDTO buyer;
  private SaleVatDTO vat;
  private SaleRedeemPointsDTO redeemPoints;

  private PaymentMethod paymentMethod;
  private Double amountPaid;
  private String paymentOrderId;

  private String employeeId;
}
