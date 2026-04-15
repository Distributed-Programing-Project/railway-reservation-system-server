package vn.edu.iuh.fit.common.enums;

public enum CarriageType {
  HARD_SEAT("Toa ngồi cứng"),
  SOFT_SEAT("Toa ngồi mềm thường"),
  SOFT_SEAT_AC("Toa ngồi mềm cao cấp"),
  BERTH_6("Toa giường 6"),
  BERTH_4("Toa giường 4");

  private final String name;

  CarriageType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
