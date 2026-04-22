package vn.edu.iuh.fit.common.constant;

public enum SeatType {
  
  HARD_SEAT("Ghế ngồi cứng"),
  SOFT_SEAT("Ghế ngồi mềm"),
  VIP_SEAT("Ghế VIP"),
  BERTH_6("Giường 6"),
  BERTH_4("Giường 4");

  private final String name;

  SeatType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
