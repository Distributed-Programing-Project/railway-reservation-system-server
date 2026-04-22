package vn.edu.iuh.fit.common.constant;

public enum CarriageType {
  HARD_SEAT("Toa ngồi cứng",   SeatType.HARD_SEAT, 64),
  SOFT_SEAT("Toa ngồi mềm thường", SeatType.SOFT_SEAT, 56),
  SOFT_SEAT_AC("Toa ngồi mềm cao cấp", SeatType.VIP_SEAT, 56),
  BERTH_6("Toa giường 6",      SeatType.BERTH_6,   42),
  BERTH_4("Toa giường 4",      SeatType.BERTH_4,   36);

  private final String name;
  private final SeatType seatType;
  private final int seatCount;

  CarriageType(String name, SeatType seatType, int seatCount) {
    this.name = name;
    this.seatType = seatType;
    this.seatCount = seatCount;
  }

  public String getName()      { return name; }
  public SeatType getSeatType(){ return seatType; }
  public int getSeatCount()    { return seatCount; }
}
