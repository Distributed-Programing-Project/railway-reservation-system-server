package vn.edu.iuh.fit.server.constant;


public enum EmployeeStatus {
    ACTIVE("Đang làm"), PAUSE("Tạm nghĩ"), INACTIVE("Đã nghĩ làm");

    private final String status;

    EmployeeStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
