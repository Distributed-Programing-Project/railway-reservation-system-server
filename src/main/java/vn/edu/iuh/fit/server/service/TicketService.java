package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.dto.ExchangeTicketRequestDTO;

public interface TicketService {
  Response exchangeTickets(ExchangeTicketRequestDTO requestDTO);
}
