package vn.edu.iuh.fit.common.message;

public final class CustomerMessages {

    private CustomerMessages() {
    }

    public static final String DATA_INVALID_PREFIX = "Dữ liệu không hợp lệ: ";
    public static final String CUSTOMER_ID_REQUIRED = "Customer ID không được để trống khi cập nhật";
    public static final String CUSTOMER_ID_MUST_BE_NULL = "Customer ID phải để trống khi thêm mới";
    public static final String CUSTOMER_NOT_FOUND = "Không tìm thấy khách hàng: id=%s";
    public static final String CUSTOMER_INACTIVE = "Không thể cập nhật khách hàng đã bị vô hiệu hóa: id=%s";
    public static final String ID_CARD_DUPLICATE = "Số CCCD này đã được đăng ký. Vui lòng kiểm tra lại";
    public static final String EMAIL_DUPLICATE = "Email này đã được đăng ký. Vui lòng kiểm tra lại";
    public static final String REQUEST_EMPLOYEE_REQUIRED = "Không tìm thấy nhân viên: id=%s";
    public static final String REQUEST_EMPLOYEE_INACTIVE = "Nhân viên yêu cầu đã bị vô hiệu hóa: id=%s";
    public static final String MANAGER_ONLY = "Bạn không có quyền thực hiện thao tác này";
    public static final String CUSTOMER_HAS_UPCOMING_TICKET = "Không thể vô hiệu hóa khách hàng đang có vé tàu sắp khởi hành";
    public static final String SEARCH_FAILED_PREFIX = "Lỗi khi tìm kiếm khách hàng: ";
    public static final String CREATE_FAILED_PREFIX = "Lỗi khi thêm khách hàng: ";
    public static final String UPDATE_FAILED_PREFIX = "Lỗi khi cập nhật khách hàng: ";
    public static final String DELETE_FAILED_PREFIX = "Lỗi khi xóa khách hàng: ";

    public static final String SEARCH_SUCCESS = "Tìm kiếm khách hàng thành công";
    public static final String CREATE_SUCCESS = "Thêm khách hàng thành công";
    public static final String UPDATE_SUCCESS = "Cập nhật khách hàng thành công";
    public static final String DELETE_SUCCESS = "Xóa khách hàng thành công";

    public static String customerNotFound(String id) {
        return String.format(CUSTOMER_NOT_FOUND, id);
    }

    public static String customerInactive(String id) {
        return String.format(CUSTOMER_INACTIVE, id);
    }

    public static String requestEmployeeNotFound(String id) {
        return String.format(REQUEST_EMPLOYEE_REQUIRED, id);
    }

    public static String requestEmployeeInactive(String id) {
        return String.format(REQUEST_EMPLOYEE_INACTIVE, id);
    }
}
