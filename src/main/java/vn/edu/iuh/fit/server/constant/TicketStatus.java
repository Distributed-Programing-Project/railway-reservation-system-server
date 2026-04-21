package vn.edu.iuh.fit.server.constant;

public enum TicketStatus {
    BOOKED("Đã đặt"),
    PAID("Đã thanh toán"),
    CANCELLED("Đã hủy"),
    USED("Đã sử dụng"),
    EXPIRED("Hết hạn"),
    EXCHANGED("Đã đổi");

    private final String name;

    TicketStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
