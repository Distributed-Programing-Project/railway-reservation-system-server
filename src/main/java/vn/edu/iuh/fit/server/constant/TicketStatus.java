package vn.edu.iuh.fit.server.constant;

public enum TicketStatus {
    BOOKED("ÄÃ£ Ä‘áº·t"),
    PAID("ÄÃ£ thanh toÃ¡n"),
    CANCELLED("ÄÃ£ há»§y"),
    USED("ÄÃ£ sá»­ dá»¥ng"),
    EXPIRED("Háº¿t háº¡n"),
    EXCHANGED("ÄÃ£ Ä‘á»•i"),
    RETURNED("Đã trả vé");

    private final String name;

    TicketStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

