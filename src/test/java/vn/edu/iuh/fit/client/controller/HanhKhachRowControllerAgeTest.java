package vn.edu.iuh.fit.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class HanhKhachRowControllerAgeTest {

  @Test
  void ageAt_shouldComputeWholeYears() {
    assertEquals(6, HanhKhachRowController.ageAt(LocalDate.of(2020, 5, 1), LocalDate.of(2026, 5, 1)));
    assertEquals(5, HanhKhachRowController.ageAt(LocalDate.of(2020, 5, 2), LocalDate.of(2026, 5, 1)));
  }

  @Test
  void ageAt_boundary60() {
    assertEquals(60, HanhKhachRowController.ageAt(LocalDate.of(1966, 5, 1), LocalDate.of(2026, 5, 1)));
    assertEquals(59, HanhKhachRowController.ageAt(LocalDate.of(1966, 5, 2), LocalDate.of(2026, 5, 1)));
  }
}

