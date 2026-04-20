package vn.edu.iuh.fit.server.repository;

import java.util.List;
import vn.edu.iuh.fit.server.model.Ticket;

public interface TicketRepository {
    boolean createTicket(Ticket ticket);
    boolean updateTicket(Ticket ticket);
    Ticket findTicketById(String id);
    List<Ticket> findTicketsByCustomerIdCard(String idCard);
}
