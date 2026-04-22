package vn.edu.iuh.fit.common.message;

public final class LoginMessages {

    private LoginMessages() {
    }

    public static final String LOGIN_DATA_REQUIRED = "Dữ liệu đăng nhập không hợp lệ";
    public static final String USERNAME_PASSWORD_REQUIRED = "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu";
    public static final String INVALID_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng";
    public static final String ACCOUNT_INACTIVE = "Tài khoản đang bị khóa hoặc đã ngừng hoạt động";
    public static final String LOGIN_SUCCESS = "Đăng nhập thành công";
    public static final String SYSTEM_ERROR_PREFIX = "Lỗi hệ thống, vui lòng thử lại: ";
}
