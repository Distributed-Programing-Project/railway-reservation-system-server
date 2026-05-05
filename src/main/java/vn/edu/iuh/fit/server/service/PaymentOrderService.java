package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.dto.PaymentCreateRequestDTO;
import vn.edu.iuh.fit.common.dto.PaymentStatusRequestDTO;
import vn.edu.iuh.fit.common.response.Response;

public interface PaymentOrderService {
  Response createPaymentOrder(PaymentCreateRequestDTO dto);

  Response getPaymentOrderStatus(PaymentStatusRequestDTO dto);

  Response confirmPaymentOrder(PaymentStatusRequestDTO dto);
}

