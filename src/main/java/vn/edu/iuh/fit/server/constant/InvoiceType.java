package vn.edu.iuh.fit.server.constant;

public enum InvoiceType {
  SALE("Bán vé"),
  REFUND("Hoàn vé"),
  EXCHANGE("Đổi vé");

  private final String name;

  InvoiceType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
