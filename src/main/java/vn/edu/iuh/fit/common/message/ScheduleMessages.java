package vn.edu.iuh.fit.common.message;

public final class ScheduleMessages {

    private ScheduleMessages() {
    }

    public static final String SCHEDULE_ID_REQUIRED = "Mã lịch trình không được để trống";
    public static final String TRAIN_ID_REQUIRED = "Mã tàu không được để trống";
    public static final String ROUTE_ID_REQUIRED = "Mã tuyến không được để trống";
    public static final String EMPLOYEE_ID_REQUIRED = "ID nhân viên không được để trống";
    public static final String ACTION_REQUIRED = "Hành động không được để trống";
    public static final String ACTION_INVALID = "Hành động không hợp lệ. Giá trị hợp lệ: PUBLISH, DISABLE";
    public static final String ARRIVAL_TIME_REQUIRED = "Ngày giờ đến dự kiến không được để trống";
    public static final String DEPARTURE_TIME_REQUIRED = "Thời gian khởi hành không được để trống";
    public static final String DEPARTURE_TIME_MIN_ONE_DAY = "Ngày khởi hành phải cách ít nhất 1 ngày so với hôm nay";
    public static final String ARRIVAL_TIME_INVALID = "Ngày giờ đến dự kiến không được nhỏ hơn giờ khởi hành";
    public static final String DATE_RANGE_INVALID = "Từ ngày không được lớn hơn Đến ngày.";
    public static final String START_TIME_AFTER_END_TIME = "Thời gian bắt đầu không được lớn hơn thời gian kết thúc";
    public static final String INVALID_STATUS_PREFIX = "Trạng thái không hợp lệ. Giá trị hợp lệ: ";
    public static final String DEPARTURE_STATION_REQUIRED = "Ga đi không được để trống";
    public static final String DESTINATION_STATION_REQUIRED = "Ga đến không được để trống";
    public static final String DRAFT_UPDATE_ONLY = "Chỉ được phép sửa lịch trình khi đang ở trạng thái Nháp";
    public static final String DRAFT_DELETE_ONLY = "Chỉ được phép xoá lịch trình khi đang ở trạng thái Nháp";
    public static final String UNAUTHORIZED = "Bạn không có quyền thực hiện thao tác này";
    public static final String DEPARTURE_TIME_IN_PAST = "Ngày hoặc giờ khởi hành ở quá khứ không hợp lệ";
    public static final String ARRIVAL_TIME_NOT_AFTER_DEPARTURE = "Ngày giờ đến dự kiến phải sau giờ khởi hành";
    public static final String EMPLOYEE_NOT_FOUND_BY_ID = "Không tìm thấy nhân viên: id=%s";
    public static final String EMPLOYEE_INACTIVE = "Nhân viên đã bị vô hiệu hóa";
    public static final String ONLY_DRAFT_CAN_BE_PUBLISHED = "Chỉ có thể phát triển lịch trình đang ở trạng thái Nháp";
    public static final String WRONG_STATUS_DISABLE = "Chỉ có thể vô hiệu hoá lịch trình đang ở trạng thái Nháp hoặc Chưa khởi hành";
    public static final String TICKETS_SOLD_BLOCKED = "Không thể vô hiệu hoá lịch trình đã có khách mua vé. Vui lòng thực hiện hoàn vé trước.";
    public static final String PRICE_NOT_CONFIGURED = "Vui lòng cập nhật giá vé cho tất cả các ghế trước khi phát triển lịch trình";

    public static final String CREATE_SUCCESS = "Tạo lịch trình thành công";
    public static final String FILTER_SUCCESS = "Lọc lịch trình thành công";
    public static final String UPDATE_SUCCESS = "Cập nhật lịch trình thành công";
    public static final String DELETE_SUCCESS = "Xoá lịch trình thành công";
    public static final String PUBLISH_SUCCESS = "Phát triển lịch trình thành công";
    public static final String DISABLE_SUCCESS = "Vô hiệu hoá lịch trình thành công";
    public static final String FIND_BY_ID_SUCCESS = "Lấy lịch trình thành công";
    public static final String FIND_ALL_SUCCESS = "Lấy danh sách lịch trình thành công";
    public static final String SEARCH_SUCCESS = "Tìm kiếm lịch trình thành công";
    public static final String FIND_BY_STATION_SUCCESS = "Lấy lịch trình theo ga thành công";

    public static final String SCHEDULE_NOT_FOUND_BY_ID = "Không tìm thấy lịch trình: id=%s";
    public static final String UPDATE_FAILED_BY_ID = "Không thể cập nhật lịch trình: id=%s";
    public static final String DELETE_FAILED_BY_ID = "Không thể xoá lịch trình: id=%s";

    public static final String CREATE_FAILED_PREFIX = "Lỗi khi tạo lịch trình: ";
    public static final String FILTER_FAILED_PREFIX = "Lỗi khi lọc lịch trình: ";
    public static final String UPDATE_FAILED_PREFIX = "Lỗi khi cập nhật lịch trình: ";
    public static final String DELETE_FAILED_PREFIX = "Lỗi khi xoá lịch trình: ";
    public static final String PUBLISH_FAILED_PREFIX = "Lỗi khi phát triển lịch trình: ";
    public static final String DISABLE_FAILED_PREFIX = "Lỗi khi vô hiệu hoá lịch trình: ";
    public static final String FIND_BY_ID_FAILED_PREFIX = "Lỗi khi tìm lịch trình: ";
    public static final String FIND_ALL_FAILED_PREFIX = "Lỗi khi lấy danh sách lịch trình: ";
    public static final String SEARCH_FAILED_PREFIX = "Lỗi khi tìm kiếm lịch trình: ";
    public static final String FIND_BY_STATION_FAILED_PREFIX = "Lỗi khi lấy lịch trình theo ga: ";

    public static String scheduleNotFoundById(String scheduleId) {
        return String.format(SCHEDULE_NOT_FOUND_BY_ID, scheduleId);
    }

    public static String updateFailedById(String scheduleId) {
        return String.format(UPDATE_FAILED_BY_ID, scheduleId);
    }

    public static String deleteFailedById(String scheduleId) {
        return String.format(DELETE_FAILED_BY_ID, scheduleId);
    }

    public static String employeeNotFoundById(String employeeId) {
        return String.format(EMPLOYEE_NOT_FOUND_BY_ID, employeeId);
    }
}
