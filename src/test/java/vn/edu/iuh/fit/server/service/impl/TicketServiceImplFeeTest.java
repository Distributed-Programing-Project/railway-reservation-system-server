package vn.edu.iuh.fit.server.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TicketServiceImplFeeTest {

  @Test
  void computeReturnFee_shouldRoundUpTo1000_andApplyMinFee() {
    // 10% of 50_000 = 5_000 -> min fee 10_000 -> already multiple of 1_000
    assertEquals(10_000.0, TicketServiceImpl.computeReturnFee(50_000.0, false, 24 * 60));

    // 10% of 123_456 = 12_345.6 -> min fee applies? no, fee=12_345.6 -> round up to 13_000
    assertEquals(13_000.0, TicketServiceImpl.computeReturnFee(123_456.0, false, 24 * 60));
  }

  @Test
  void computeReturnFee_shouldUse20PercentUnder24h() {
    // minutesToDeparture < 24h => 20%
    // 20% of 100_000 = 20_000 -> ok
    assertEquals(20_000.0, TicketServiceImpl.computeReturnFee(100_000.0, false, 23 * 60));
  }

  @Test
  void computeReturnFee_shouldUse10PercentAtOrOver24h() {
    // minutesToDeparture >= 24h => 10%
    assertEquals(10_000.0, TicketServiceImpl.computeReturnFee(100_000.0, false, 24 * 60));
  }

  @Test
  void computeReturnFee_shouldUse30PercentForExchangedTickets() {
    // exchanged => 30%
    assertEquals(30_000.0, TicketServiceImpl.computeReturnFee(100_000.0, true, 48 * 60));
  }

  @Test
  void computeReturnFee_shouldNotExceedTicketPrice() {
    // 30% of 20_000 = 6_000 -> min fee 10_000 -> ok
    assertEquals(10_000.0, TicketServiceImpl.computeReturnFee(20_000.0, true, 48 * 60));

    // extremely small price => fee capped at price
    assertEquals(9_000.0, TicketServiceImpl.computeReturnFee(9_000.0, false, 48 * 60));
  }
}

