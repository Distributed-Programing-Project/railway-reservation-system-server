package vn.edu.iuh.fit.common.message;

public final class TrainMessages {
    private TrainMessages() {}

    public static final String NOT_FOUND = "Không tìm thấy tàu: id=%s";
    public static final String FIND_ALL_SUCCESS = "Lấy danh sách tàu thành công";
    public static final String FIND_BY_ID_SUCCESS = "Lấy thông tin tàu thành công";

    public static final String FIND_BY_CODE_BLANK = "Mác tàu tìm kiếm không được để trống";
    public static final String FIND_BY_CODE_SUCCESS = "Tìm kiếm tàu thành công";
    public static final String FIND_UNASSIGNED_SUCCESS = "Lấy danh sách toa rỗng thành công";

    public static final String TRAIN_CODE_DUPLICATE = "Mác tàu %s đã tồn tại, vui lòng chọn mác khác";
    public static final String CREATE_TRAIN_SUCCESS = "Tàu %s được tạo thành công";
    public static final String CREATE_CARRIAGE_SUCCESS = "Toa mới đã được đăng ký vào hệ thống";

    public static final String HAS_FUTURE_SCHEDULES = "Không thể thay đổi tàu đang có lịch trình trong tương lai. Vui lòng huỷ các lịch trình liên quan trước";
    public static final String UPDATE_CARRIAGES_SUCCESS = "Cấu hình tàu đã được cập nhật";
    public static final String UPDATE_STATUS_SUCCESS = "Trạng thái tàu đã được cập nhật";

    public static final String SYSTEM_ERROR = "Lỗi hệ thống, vui lòng thử lại";
}
