package vn.edu.iuh.fit.server.messages;

public final class TicketMessages {

    private TicketMessages() {
    }

    public static final String TICKET_ID_REQUIRED = "Mã vé không được để trống";
    public static final String SEAT_NOT_AVAILABLE = "Ghế số %s của chuyến %s đã có người đặt.";
    public static final String TICKET_ALREADY_EXCHANGED = "Vé %s đã từng được đổi trước đó.";
    public static final String TICKET_NOT_PAID = "Vé %s không ở trạng thái hợp lệ để đổi.";
    public static final String EXCHANGE_TIME_EXPIRED = "Vé %s không được đổi vì chỉ còn %d h tới giờ khởi hành (yêu cầu ít nhất 24h).";
    
    public static final String CREATE_SUCCESS = "Tạo vé thành công";
    public static final String EXCHANGE_SUCCESS = "Đổi vé thành công. Số tiền thanh toán: %.2f";
    public static final String FIND_SUCCESS = "Lấy thông tin vé thành công";
    
    public static final String TICKET_NOT_FOUND = "Không tìm thấy vé: id=%s";
    public static final String CREATE_FAILED = "Lỗi hệ thống: Không thể tạo vé mới.";
    public static final String EXCHANGE_FAILED_PREFIX = "Lỗi nghiệp vụ đổi vé: ";
    public static final String DATA_INVALID_PREFIX = "Dữ liệu không hợp lệ: ";
    public static final String COUNT_MISMATCH = "Số lượng vé cũ và ghế mới phải khớp nhau.";
    public static final String SOME_TICKETS_INVALID = "Một số vé không tồn tại hoặc không hợp lệ.";

    public static String ticketNotFound(String id) {
        return String.format(TICKET_NOT_FOUND, id);
    }
}
