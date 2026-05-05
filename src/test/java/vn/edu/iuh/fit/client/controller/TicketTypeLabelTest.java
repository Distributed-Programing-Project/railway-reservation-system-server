package vn.edu.iuh.fit.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import vn.edu.iuh.fit.common.constant.TicketType;

class TicketTypeLabelTest {

  @Test
  void ticketTypeLabel_shouldReturnVietnamese() {
    assertEquals("Vé người lớn", HanhKhachRowController.ticketTypeLabel(TicketType.NORMAL));
    assertEquals("Vé trẻ em", HanhKhachRowController.ticketTypeLabel(TicketType.CHILD));
    assertEquals("Vé người lớn tuổi", HanhKhachRowController.ticketTypeLabel(TicketType.SENIOR));
    assertEquals("Vé học sinh - sinh viên", HanhKhachRowController.ticketTypeLabel(TicketType.STUDENT));
  }
}

