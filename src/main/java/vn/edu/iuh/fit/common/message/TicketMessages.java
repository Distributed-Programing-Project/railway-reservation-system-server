package vn.edu.iuh.fit.common.message;

public final class TicketMessages {

    private TicketMessages() {
    }

    public static final String TICKET_ID_REQUIRED = "Mã vé không được để trống";
    public static final String OLD_TICKET_IDS_REQUIRED = "Danh sách vé cũ không được để trống";
    public static final String NEW_SCHEDULE_DETAIL_IDS_REQUIRED = "Danh sách ghế mới không được để trống";
    public static final String CASH_RECEIVED_NOT_NEGATIVE = "Số tiền thực nhận không được âm";
    public static final String SEAT_NOT_AVAILABLE = "Ghế số %s của chuyến %s đã có người đặt.";
    public static final String TICKET_ALREADY_EXCHANGED = "Vé %s đã từng được đổi trước đó.";
    public static final String TICKET_NOT_PAID = "Vé %s không ở trạng thái hợp lệ để đổi.";
    public static final String TICKET_ALREADY_RETURNED = "Vé %s đã được trả, không thể đổi.";
    public static final String EXCHANGE_TIME_EXPIRED = "Vé %s không được đổi vì chỉ còn %d h tới giờ khởi hành (yêu cầu ít nhất 24h).";
    
    public static final String CREATE_SUCCESS = "Tạo vé thành công";
    public static final String EXCHANGE_SUCCESS = "Đổi vé thành công. Số tiền thanh toán: %.2f";
    public static final String FIND_SUCCESS = "Lấy thông tin vé thành công";

    public static final String EXCHANGE_SEARCH_SUCCESS = "Tra cứu vé đổi thành công";
    public static final String EXCHANGE_SEARCH_FAILED_PREFIX = "Lỗi khi tìm kiếm vé đổi: ";
    public static final String EXCHANGE_PREVIEW_SUCCESS = "Tính phí đổi vé thành công";
    public static final String EXCHANGE_PREVIEW_FAILED_PREFIX = "Lỗi khi xem trước phí đổi vé: ";
    
    public static final String TICKET_NOT_FOUND = "Không tìm thấy vé: id=%s";
    public static final String CREATE_FAILED = "Lỗi hệ thống: Không thể tạo vé mới.";
    public static final String EXCHANGE_FAILED_PREFIX = "Lỗi nghiệp vụ đổi vé: ";
    public static final String DATA_INVALID_PREFIX = "Dữ liệu không hợp lệ: ";
    public static final String COUNT_MISMATCH = "Số lượng vé cũ và ghế mới phải khớp nhau.";
    public static final String SOME_TICKETS_INVALID = "Một số vé không tồn tại hoặc không hợp lệ.";
    public static final String ID_CARD_REQUIRED = "Số CCCD không được để trống";
    public static final String REFUND_AMOUNT_INVALID = "Số tiền hoàn trả phải lớn hơn hoặc bằng 0";
    public static final String DATA_CONFLICT = "Ghế bạn chọn vừa có người khác đặt nhanh hơn. Vui lòng thử lại!";
    public static final String SCHEDULE_DETAIL_NOT_FOUND = "Không tìm thấy chi tiết lịch trình: %s";
    
    // Return Ticket
    public static final String RETURN_SUCCESS = "Trả vé thành công";
    public static final String PREVIEW_SUCCESS = "Tính toán số tiền hoàn trả thành công";
    public static final String SEARCH_FAILED_PREFIX = "Lỗi khi tìm kiếm vé trả: ";
    public static final String PREVIEW_FAILED_PREFIX = "Lỗi khi xem trước số tiền hoàn: ";
    public static final String RETURN_FAILED_PREFIX = "Lỗi khi thực hiện trả vé: ";
    public static final String TICKET_NOT_RETURNABLE = "Vé %s không ở trạng thái hợp lệ để trả.";
    public static final String NOT_ELIGIBLE_BY_TIME = "Vé %s không đủ điều kiện trả vì thời gian khởi hành còn dưới 4 tiếng.";
    public static final String REFUND_AMOUNT_MISMATCH = "Số tiền hoàn trả không khớp với tính toán của hệ thống.";
    public static final String CUSTOMER_MISMATCH = "Tất cả các vé phải thuộc cùng một khách hàng để thực hiện trả theo lô.";
    public static final String TICKET_IDS_REQUIRED = "Danh sách mã vé không được để trống.";
    public static final String EMPLOYEE_ID_REQUIRED = "Employee ID không được để trống";
    public static final String INVALID_SESSION = "Phiên làm việc không hợp lệ.";
    public static final String TAX_CODE_TOO_LONG = "Mã số thuế không được vượt quá 20 ký tự";
    public static final String COMPANY_NAME_TOO_LONG = "Tên công ty không được vượt quá 200 ký tự";
    public static final String TICKET_IDS_DUPLICATE = "Danh sách mã vé chứa các giá trị trùng lặp.";
    public static final String SCHEDULE_NOT_FOUND = "Không tìm thấy thông tin lịch trình cho vé này.";

    public static final String SEAT_HELD_BY_OTHER = "Ghế bạn chọn đang được giữ chỗ bởi phiên khác. Vui lòng chọn ghế khác.";

    public static String ticketNotFound(String id) {
        return String.format(TICKET_NOT_FOUND, id);
    }

    public static String scheduleDetailNotFound(String id) {
        return String.format(SCHEDULE_DETAIL_NOT_FOUND, id);
    }
}
