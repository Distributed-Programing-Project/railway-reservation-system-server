package vn.edu.iuh.fit.common.message;

public final class SaleMessages {
  private SaleMessages() {
  }

  public static final String STATION_LIST_SUCCESS = "Lấy danh sách ga thành công";
  public static final String SEARCH_SCHEDULE_SUCCESS = "Tìm chuyến tàu thành công";
  public static final String SEATMAP_SUCCESS = "Lấy sơ đồ chỗ ngồi thành công";
  public static final String SALE_SUCCESS = "Bán vé thành công";
  public static final String HOLD_SUCCESS = "Giữ ghế thành công";
  public static final String RELEASE_HOLD_SUCCESS = "Nhả ghế thành công";

  public static final String INVALID_REQUEST = "Dữ liệu yêu cầu không hợp lệ";
  public static final String INVALID_STATIONS = "Ga đi và ga đến không hợp lệ";
  public static final String INVALID_DATES = "Ngày đi/ngày về không hợp lệ";
  public static final String NOT_FOUND_SCHEDULE = "Không tìm thấy chuyến tàu phù hợp";
  public static final String SEAT_CONSTRAINT_MISMATCH = "Số lượng ghế chiều đi và chiều về phải bằng nhau";
  public static final String TOO_MANY_TICKETS_PER_LEG = "Mỗi chiều chỉ được mua tối đa 10 vé";
  public static final String SEAT_ALREADY_SOLD = "Ghế đã được bán bởi giao dịch khác, vui lòng chọn lại";
  public static final String SEAT_HELD_BY_OTHER = "Ghế đang được giao dịch bởi quầy khác";
  public static final String PRICE_NOT_CONFIGURED = "Giá ghế chưa được cấu hình";
  public static final String CUSTOMER_DOCUMENT_REQUIRED = "Cần CCCD/CMND hoặc hộ chiếu";
  public static final String PAYMENT_NOT_READY = "Chưa đủ điều kiện thanh toán";
  public static final String PAYMENT_CASH_ONLY = "Chỉ hỗ trợ thanh toán tiền mặt";

  public static final String ONLINE_PAYMENT_ORDER_REQUIRED = "Cần tạo đơn thanh toán online";
  public static final String ONLINE_PAYMENT_ORDER_NOT_FOUND = "Không tìm thấy đơn thanh toán online";
  public static final String ONLINE_PAYMENT_NOT_CONFIRMED = "Thanh toán online chưa được xác nhận";
  public static final String ONLINE_PAYMENT_ALREADY_USED = "Đơn thanh toán online đã được sử dụng";
  public static final String ONLINE_PAYMENT_SESSION_MISMATCH = "Phiên giao dịch không khớp với đơn thanh toán";
  public static final String ONLINE_PAYMENT_AMOUNT_MISMATCH = "Số tiền đơn thanh toán không khớp tổng tiền";
  public static final String ONLINE_PAYMENT_CONSUME_FAILED = "Không thể chốt đơn thanh toán online";

  public static final String CHILD_REQUIRES_ADULT = "Vé trẻ em bắt buộc phải có người lớn đi kèm";
  public static final String POINTS_NOT_ALLOWED_WITH_DISCOUNT = "Không được đổi điểm khi có vé ưu đãi đối tượng";
}
