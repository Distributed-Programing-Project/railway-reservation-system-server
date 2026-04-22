package vn.edu.iuh.fit.common.message;

public final class EmployeeMessages {

    private EmployeeMessages() {
    }

    public static final String EMPLOYEE_ID_REQUIRED = "ID nhân viên không được để trống";
    public static final String NAME_REQUIRED = "Họ tên không được để trống";
    public static final String NATIONAL_ID_REQUIRED = "CCCD không được để trống";
    public static final String NATIONAL_ID_FORMAT = "CCCD phải gồm 9 hoặc 12 chữ số";
    public static final String DATE_OF_BIRTH_REQUIRED = "Ngày sinh không được để trống";
    public static final String PHONE_REQUIRED = "Số điện thoại không được để trống";
    public static final String PHONE_FORMAT = "Số điện thoại phải gồm đúng 10 chữ số";
    public static final String EMAIL_REQUIRED = "Email không được để trống";
    public static final String EMAIL_FORMAT = "Email không đúng định dạng";
    public static final String IS_MANAGER_REQUIRED = "Loại nhân viên không được để trống";
    public static final String PAGE_NOT_NEGATIVE = "Page không được âm";
    public static final String PAGE_SIZE_MIN = "Page size phải lớn hơn 0";
    public static final String NOT_FOUND_BY_ID = "Không tìm thấy nhân viên: id=%s";
    public static final String NOT_FOUND_BY_PHONE = "Không tìm thấy nhân viên với số điện thoại: %s";
    public static final String ACCESS_DENIED = "Bạn không có quyền thực hiện thao tác này";
    public static final String EMAIL_ALREADY_EXISTS = "Email đã được sử dụng bởi tài khoản khác";
    public static final String NATIONAL_ID_ALREADY_EXISTS = "CCCD đã được đăng ký cho nhân viên khác";
    public static final String ACCOUNT_ALREADY_EXISTS = "Nhân viên đã được cấp tài khoản";
    public static final String ACCOUNT_NOT_EXISTS = "Nhân viên chưa được cấp tài khoản";
    public static final String ALREADY_INACTIVE = "Nhân viên này đã bị xoá khỏi hệ thống";

    public static final String CREATE_SUCCESS = "Tạo nhân viên thành công";
    public static final String UPDATE_SUCCESS = "Cập nhật nhân viên thành công";
    public static final String DELETE_SUCCESS = "Xóa nhân viên thành công";
    public static final String SOFT_DELETE_SUCCESS = "Xoá mềm nhân viên thành công";
    public static final String FIND_SUCCESS = "Lấy thông tin nhân viên thành công";
    public static final String FIND_ALL_SUCCESS = "Lấy danh sách nhân viên thành công";
    public static final String ACCOUNT_CREATE_SUCCESS = "Cấp tài khoản thành công";
    public static final String PASSWORD_RESET_SUCCESS = "Reset mật khẩu thành công";

    public static final String SYSTEM_ERROR_PREFIX = "Lỗi hệ thống, vui lòng thử lại: ";
    public static final String CREATE_FAILED_PREFIX = "Lỗi khi tạo nhân viên: ";

    public static String notFoundById(String id) {
        return String.format(NOT_FOUND_BY_ID, id);
    }
}
