package vn.edu.iuh.fit.server.payment;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import vn.edu.iuh.fit.common.constant.PaymentStatus;

public final class InternalPaymentOrderStore {

  private static final SecureRandom random = new SecureRandom();
  private static final Map<String, PaymentOrder> store = new ConcurrentHashMap<>();
  private static final int REF_LEN = 10;

  private InternalPaymentOrderStore() {
  }

  public static PaymentOrder createOrder(double amount, String description, String clientSessionId) {
    String orderId = UUID.randomUUID().toString();
    String referenceCode = generateReferenceCode();
    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
    PaymentOrder order = new PaymentOrder(
        orderId,
        referenceCode,
        amount,
        description,
        PaymentStatus.PENDING,
        LocalDateTime.now(),
        expiresAt,
        null,
        clientSessionId,
        null,
        null);
    store.put(orderId, order);
    return order;
  }

  public static PaymentOrder get(String orderId) {
    if (orderId == null) return null;
    PaymentOrder order = store.get(orderId);
    if (order == null) return null;
    PaymentOrder normalized = expireIfNeeded(order);
    store.put(orderId, normalized);
    return normalized;
  }

  public static PaymentOrder confirmOrder(String orderId, String clientSessionId) {
    AtomicReference<PaymentOrder> ref = new AtomicReference<>();
    store.computeIfPresent(orderId, (id, current) -> {
      PaymentOrder order = expireIfNeeded(current);
      if (order.status == PaymentStatus.EXPIRED || order.status == PaymentStatus.SUCCESS) {
        ref.set(order);
        return order;
      }
      if (order.clientSessionId == null || clientSessionId == null || !order.clientSessionId.equals(clientSessionId)) {
        ref.set(null);
        return order;
      }
      PaymentOrder updated = order.withStatus(PaymentStatus.SUCCESS, LocalDateTime.now());
      ref.set(updated);
      return updated;
    });
    return ref.get();
  }

  public static PaymentOrder consumeOrder(String orderId, String invoiceId, String clientSessionId) {
    AtomicReference<PaymentOrder> ref = new AtomicReference<>();
    store.computeIfPresent(orderId, (id, current) -> {
      PaymentOrder order = expireIfNeeded(current);
      if (order.status != PaymentStatus.SUCCESS) {
        ref.set(null);
        return order;
      }
      if (order.consumedInvoiceId != null) {
        ref.set(order);
        return order;
      }
      if (order.clientSessionId == null || clientSessionId == null || !order.clientSessionId.equals(clientSessionId)) {
        ref.set(null);
        return order;
      }
      PaymentOrder updated = order.withConsumed(invoiceId, LocalDateTime.now());
      ref.set(updated);
      return updated;
    });
    return ref.get();
  }

  public static String buildQrPayload(PaymentOrder order) {
    String desc = order.description != null ? order.description : "Thanh toán vé tàu";
    return String.format(Locale.ROOT, "INTERNAL_TRANSFER|ORDER=%s|AMOUNT=%.0f|REF=%s|DESC=%s",
        order.orderId, order.amount, order.referenceCode, desc);
  }

  private static PaymentOrder expireIfNeeded(PaymentOrder order) {
    if (order.status == PaymentStatus.PENDING && order.expiresAt != null && order.expiresAt.isBefore(LocalDateTime.now())) {
      return order.withStatus(PaymentStatus.EXPIRED, null);
    }
    return order;
  }

  private static String generateReferenceCode() {
    String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder sb = new StringBuilder(REF_LEN);
    for (int i = 0; i < REF_LEN; i++) {
      sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return sb.toString();
  }

  public record PaymentOrder(
      String orderId,
      String referenceCode,
      double amount,
      String description,
      PaymentStatus status,
      LocalDateTime createdAt,
      LocalDateTime expiresAt,
      LocalDateTime paidAt,
      String clientSessionId,
      String consumedInvoiceId,
      LocalDateTime consumedAt) {
    public PaymentOrder withStatus(PaymentStatus newStatus, LocalDateTime paidAt) {
      return new PaymentOrder(orderId, referenceCode, amount, description, newStatus, createdAt, expiresAt, paidAt,
          clientSessionId, consumedInvoiceId, consumedAt);
    }

    public PaymentOrder withConsumed(String invoiceId, LocalDateTime at) {
      return new PaymentOrder(orderId, referenceCode, amount, description, status, createdAt, expiresAt, paidAt,
          clientSessionId, invoiceId, at);
    }
  }
}
