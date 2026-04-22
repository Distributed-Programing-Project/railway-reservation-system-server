package vn.edu.iuh.fit.common.constant;

public enum TrainStatus {
    ACTIVE("Đang hoạt động"),
    MAINTENANCE("Đang bảo trì"),
    INACTIVE("Ngừng hoạt động");

    private final String name;

    TrainStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
