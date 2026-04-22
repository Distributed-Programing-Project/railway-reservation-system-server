package vn.edu.iuh.fit.common.constant;

public enum TicketType {
  NORMAL("Vé bình thường"),
  SENIOR("Vé người lớn tuổi"),
  CHILD("Vé trẻ em"),
  STUDENT("Vé học sinh - sinh viên");

  private final String name;

  TicketType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
