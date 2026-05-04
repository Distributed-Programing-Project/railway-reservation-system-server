package vn.edu.iuh.fit.server.service.impl;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import vn.edu.iuh.fit.common.constant.PaymentStatus;
import vn.edu.iuh.fit.common.dto.PaymentCreateRequestDTO;
import vn.edu.iuh.fit.common.dto.PaymentCreateResponseDTO;
import vn.edu.iuh.fit.common.dto.PaymentStatusDTO;
import vn.edu.iuh.fit.common.dto.PaymentStatusRequestDTO;
import vn.edu.iuh.fit.common.message.PaymentMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.payment.InternalPaymentOrderStore;
import vn.edu.iuh.fit.server.service.PaymentOrderService;

public class PaymentOrderServiceImpl implements PaymentOrderService {

  @Override
  public Response createPaymentOrder(PaymentCreateRequestDTO dto) {
    if (dto == null || dto.getAmount() <= 0) {
      return Response.error(PaymentMessages.INVALID_AMOUNT);
    }
    String sessionId = normalize(dto.getClientSessionId());
    if (sessionId == null) {
      return Response.error(PaymentMessages.INVALID_SESSION);
    }

    InternalPaymentOrderStore.PaymentOrder order = InternalPaymentOrderStore.createOrder(dto.getAmount(), normalize(dto.getDescription()), sessionId);
    String payload = InternalPaymentOrderStore.buildQrPayload(order);
    byte[] qrPng = generateQrPng(payload);

    PaymentCreateResponseDTO responseDTO = PaymentCreateResponseDTO.builder()
        .paymentOrderId(order.orderId())
        .referenceCode(order.referenceCode())
        .qrPayload(payload)
        .qrPng(qrPng)
        .status(order.status())
        .expiresAt(order.expiresAt())
        .build();
    return Response.success(PaymentMessages.CREATE_SUCCESS, responseDTO);
  }

  @Override
  public Response getPaymentOrderStatus(PaymentStatusRequestDTO dto) {
    if (dto == null || dto.getPaymentOrderId() == null || dto.getPaymentOrderId().isBlank()) {
      return Response.error(PaymentMessages.ORDER_NOT_FOUND);
    }
    InternalPaymentOrderStore.PaymentOrder order = InternalPaymentOrderStore.get(dto.getPaymentOrderId().trim());
    if (order == null) {
      return Response.error(PaymentMessages.ORDER_NOT_FOUND);
    }
    PaymentStatusDTO statusDTO = PaymentStatusDTO.builder()
        .paymentOrderId(order.orderId())
        .status(order.status())
        .message(order.status() == PaymentStatus.EXPIRED ? PaymentMessages.ORDER_EXPIRED : null)
        .paidAt(order.paidAt())
        .build();
    return Response.success(PaymentMessages.STATUS_SUCCESS, statusDTO);
  }

  @Override
  public Response confirmPaymentOrder(PaymentStatusRequestDTO dto) {
    if (dto == null || dto.getPaymentOrderId() == null || dto.getPaymentOrderId().isBlank()) {
      return Response.error(PaymentMessages.ORDER_NOT_FOUND);
    }
    String orderId = dto.getPaymentOrderId().trim();
    String sessionId = normalize(dto.getClientSessionId());
    if (sessionId == null) {
      return Response.error(PaymentMessages.INVALID_SESSION);
    }

    InternalPaymentOrderStore.PaymentOrder order = InternalPaymentOrderStore.get(orderId);
    if (order == null) {
      return Response.error(PaymentMessages.ORDER_NOT_FOUND);
    }
    if (order.status() == PaymentStatus.EXPIRED) {
      return Response.error(PaymentMessages.ORDER_EXPIRED);
    }
    if (order.clientSessionId() == null || !sessionId.equals(order.clientSessionId())) {
      return Response.error(PaymentMessages.SESSION_MISMATCH);
    }

    InternalPaymentOrderStore.PaymentOrder confirmed = InternalPaymentOrderStore.confirmOrder(orderId, sessionId);
    if (confirmed == null) {
      return Response.error(PaymentMessages.SESSION_MISMATCH);
    }

    PaymentStatusDTO statusDTO = PaymentStatusDTO.builder()
        .paymentOrderId(confirmed.orderId())
        .status(confirmed.status())
        .paidAt(confirmed.paidAt())
        .build();
    return Response.success(PaymentMessages.CONFIRM_SUCCESS, statusDTO);
  }

  private byte[] generateQrPng(String payload) {
    if (payload == null || payload.isBlank()) return null;
    try {
      Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.MARGIN, 1);
      BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 240, 240, hints);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
      return baos.toByteArray();
    } catch (Exception e) {
      return null;
    }
  }

  private String normalize(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
