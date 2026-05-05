package vn.edu.iuh.fit.common.message;

public final class PaymentMessages {
  private PaymentMessages() {
  }

  public static final String CREATE_SUCCESS = "Tạo thanh toán online thành công";
  public static final String STATUS_SUCCESS = "Lấy trạng thái thanh toán thành công";
  public static final String CONFIRM_SUCCESS = "Xác nhận thanh toán thành công";

  public static final String INVALID_SESSION = "Phiên giao dịch không hợp lệ";
  public static final String SESSION_MISMATCH = "Phiên giao dịch không khớp";
  public static final String INVALID_AMOUNT = "Số tiền thanh toán không hợp lệ";
  public static final String ORDER_NOT_FOUND = "Không tìm thấy đơn thanh toán";
  public static final String ORDER_EXPIRED = "Đơn thanh toán đã hết hạn";
}
