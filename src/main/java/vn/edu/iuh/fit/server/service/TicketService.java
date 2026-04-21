package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.dto.ExchangeTicketRequestDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketConfirmDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketPreviewRequestDTO;
import vn.edu.iuh.fit.server.dto.ReturnTicketSearchDTO;

public interface TicketService {
  Response exchangeTickets(ExchangeTicketRequestDTO requestDTO);

  Response searchTicketsForReturn(ReturnTicketSearchDTO searchDTO);

  Response previewReturnTickets(ReturnTicketPreviewRequestDTO previewRequestDTO);

  Response confirmReturnTickets(ReturnTicketConfirmDTO confirmDTO);
}
