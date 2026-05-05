package vn.edu.iuh.fit.common.message;

public final class InvoiceMessages {

    private InvoiceMessages() {}

    // Validation
    public static final String REQUEST_EMPLOYEE_ID_REQUIRED = "Mã nhân viên yêu cầu không được để trống";
    public static final String DAY_INVALID = "Ngày phải từ 1 đến 31";
    public static final String MONTH_INVALID = "Tháng phải từ 1 đến 12";
    public static final String YEAR_INVALID = "Năm phải từ 2000 trở lên";
    public static final String INVOICE_ID_REQUIRED = "Mã hoá đơn không được để trống";

    // Success
    public static final String FILTER_SUCCESS = "Lấy danh sách hoá đơn thành công";
    public static final String DETAIL_SUCCESS = "Lấy chi tiết hoá đơn thành công";

    // Error
    public static final String NOT_FOUND = "Không tìm thấy hoá đơn: id=%s";
    public static final String EMPLOYEE_NOT_FOUND = "Không tìm thấy nhân viên: id=%s";
    public static final String EMPLOYEE_INACTIVE = "Nhân viên không còn hoạt động: id=%s";
    public static final String FILTER_FAILED = "Lỗi khi lấy danh sách hoá đơn: ";
    public static final String DETAIL_FAILED = "Lỗi khi lấy chi tiết hoá đơn: ";

    public static String notFound(String id) {
        return String.format(NOT_FOUND, id);
    }

    public static String employeeNotFound(String id) {
        return String.format(EMPLOYEE_NOT_FOUND, id);
    }

    public static String employeeInactive(String id) {
        return String.format(EMPLOYEE_INACTIVE, id);
    }
}
