package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.common.dto.ExchangeEligibleTicketSearchDTO;
import vn.edu.iuh.fit.common.dto.ExchangeTicketRequestDTO;
import vn.edu.iuh.fit.common.dto.ExchangeTicketPreviewRequestDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketConfirmDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketPreviewRequestDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketSearchDTO;

public interface TicketService {
  Response exchangeTickets(ExchangeTicketRequestDTO requestDTO);

  Response searchTicketsForExchange(ExchangeEligibleTicketSearchDTO searchDTO);

  Response previewExchangeTickets(ExchangeTicketPreviewRequestDTO previewDTO);

  Response searchTicketsForReturn(ReturnTicketSearchDTO searchDTO);

  Response previewReturnTickets(ReturnTicketPreviewRequestDTO previewRequestDTO);

  Response confirmReturnTickets(ReturnTicketConfirmDTO confirmDTO);
}
