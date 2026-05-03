package vn.edu.iuh.fit.common.message;

public final class RouteMessages {
    private RouteMessages() {}

    public static final String NOT_FOUND = "Không tìm thấy tuyến: id=%s";
    public static final String EMPLOYEE_ID_REQUIRED = "ID nhân viên không được để trống";
    public static final String ROUTE_ID_REQUIRED = "Mã tuyến không được để trống";
    public static final String ROUTE_CODE_REQUIRED = "Mã tuyến đường không được để trống";
    public static final String DEPARTURE_STATION_REQUIRED = "Ga đi không được để trống";
    public static final String DESTINATION_STATION_REQUIRED = "Ga đến không được để trống";
    public static final String STATION_STOP_REQUIRED = "Ga dừng không được để trống";
    public static final String PRICE_INVALID = "Giá cơ bản không hợp lệ";
    public static final String SAME_STATION = "Ga đi và ga đến không được trùng nhau";
    public static final String STOP_DUPLICATED = "Danh sách ga dừng không được trùng nhau";
    public static final String STOP_MATCHES_ENDPOINT = "Ga dừng không được trùng ga đi hoặc ga đến";
    public static final String STOP_ORDER_INVALID = "Thứ tự ga dừng không hợp lệ";
    public static final String STATION_NOT_FOUND = "Không tìm thấy ga: id=%s";
    public static final String DUPLICATE_ROUTE = "Đã có tuyến đường với cặp ga đi/ga đến này";
    public static final String DUPLICATE_ROUTE_OTHER = "Đã tồn tại tuyến đường khác với cặp ga này";
    public static final String HAS_SCHEDULES_UPDATE = "Không thể sửa tuyến đường đã gắn lịch trình";
    public static final String HAS_SCHEDULES_DELETE = "Không thể xoá tuyến đường đã có lịch trình";
    public static final String HAS_SCHEDULES_DISABLE = "Không thể vô hiệu hoá tuyến đường đã có lịch trình";
    public static final String DRAFT_DELETE_ONLY = "Chỉ được xoá tuyến đường ở trạng thái Nháp";
    public static final String DRAFT_PROMOTE_ONLY = "Chỉ có thể phát triển tuyến đường đang ở trạng thái Nháp";
    public static final String ACTIVE_DISABLE_ONLY = "Chỉ có thể vô hiệu hoá tuyến đang hoạt động";
    public static final String UNAUTHORIZED = "Bạn không có quyền thực hiện thao tác này";
    public static final String EMPLOYEE_INACTIVE = "Nhân viên đã bị vô hiệu hóa";
    public static final String EMPLOYEE_NOT_FOUND_BY_ID = "Không tìm thấy nhân viên: id=%s";

    public static final String FIND_ALL_SUCCESS = "Lấy danh sách tuyến thành công";
    public static final String FIND_ALL_FAILED = "Lấy danh sách tuyến thất bại: ";
    public static final String FIND_BY_ID_SUCCESS = "Lấy thông tin tuyến thành công";
    public static final String FILTER_SUCCESS = "Lọc tuyến đường thành công";
    public static final String FILTER_EMPTY = "Không tìm thấy tuyến phù hợp";
    public static final String CREATE_SUCCESS = "Tạo tuyến đường thành công";
    public static final String UPDATE_SUCCESS = "Cập nhật tuyến đường thành công";
    public static final String DELETE_SUCCESS = "Xoá tuyến đường nháp thành công";
    public static final String PROMOTE_SUCCESS = "Phát triển tuyến đường thành công";
    public static final String DISABLE_SUCCESS = "Vô hiệu hoá tuyến đường thành công";
    public static final String FIND_STOPS_SUCCESS = "Lấy danh sách ga dừng thành công";
    public static final String CREATE_STOP_SUCCESS = "Tạo ga dừng thành công";
    public static final String UPDATE_STOP_SUCCESS = "Cập nhật ga dừng thành công";
    public static final String DELETE_STOP_SUCCESS = "Xoá ga dừng thành công";

    public static final String FIND_BY_ID_FAILED_PREFIX = "Lỗi khi lấy thông tin tuyến: ";
    public static final String FILTER_FAILED_PREFIX = "Lỗi khi lọc tuyến đường: ";
    public static final String CREATE_FAILED_PREFIX = "Lỗi khi tạo tuyến đường: ";
    public static final String UPDATE_FAILED_PREFIX = "Lỗi khi cập nhật tuyến đường: ";
    public static final String DELETE_FAILED_PREFIX = "Lỗi khi xoá tuyến đường: ";
    public static final String PROMOTE_FAILED_PREFIX = "Lỗi khi phát triển tuyến đường: ";
    public static final String DISABLE_FAILED_PREFIX = "Lỗi khi vô hiệu hoá tuyến đường: ";
    public static final String ROUTE_STOP_FAILED_PREFIX = "Lỗi khi xử lý ga dừng: ";

    public static String notFound(String routeId) {
        return String.format(NOT_FOUND, routeId);
    }

    public static String stationNotFound(String stationId) {
        return String.format(STATION_NOT_FOUND, stationId);
    }

    public static String employeeNotFoundById(String employeeId) {
        return String.format(EMPLOYEE_NOT_FOUND_BY_ID, employeeId);
    }
}
