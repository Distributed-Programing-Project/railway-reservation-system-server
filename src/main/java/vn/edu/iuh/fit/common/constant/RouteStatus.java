package vn.edu.iuh.fit.common.constant;

public enum RouteStatus {
    DRAFT("Nháp"),
    ACTIVE("Đang hoạt động"),
    PAUSED("Tạm dừng"),
    CANCELLED("Đã hủy");

    private final String name;

    RouteStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
