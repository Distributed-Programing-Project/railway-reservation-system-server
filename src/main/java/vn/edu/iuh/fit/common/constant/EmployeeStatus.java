package vn.edu.iuh.fit.common.constant;


public enum EmployeeStatus {
    ACTIVE("Đang làm"), PAUSE("Tạm nghỉ"), INACTIVE("Đã nghỉ làm");

    private final String status;

    EmployeeStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
