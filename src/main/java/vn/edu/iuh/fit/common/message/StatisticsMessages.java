package vn.edu.iuh.fit.common.message;

public final class StatisticsMessages {
    private StatisticsMessages() {}
    public static final String PERIOD_TYPE_REQUIRED = "Loại kỳ thống kê là bắt buộc";
    public static final String TARGET_DATE_REQUIRED = "Ngày đại diện kỳ là bắt buộc";
    public static final String EMPLOYEE_ID_REQUIRED = "ID nhân viên yêu cầu là bắt buộc";
    public static final String REQUEST_EMPLOYEE_NOT_FOUND = "Nhân viên yêu cầu không tồn tại: %s";
    public static final String REQUEST_EMPLOYEE_INACTIVE = "Nhân viên yêu cầu đã bị vô hiệu hóa";
    public static final String NOT_MANAGER = "Chỉ quản lý mới có quyền xem thống kê";
    public static final String GET_SUCCESS = "Thống kê thành công";
    public static final String SYSTEM_ERROR = "Lỗi hệ thống khi truy vấn thống kê";
}
