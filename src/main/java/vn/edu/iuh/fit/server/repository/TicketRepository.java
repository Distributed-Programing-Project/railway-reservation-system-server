package vn.edu.iuh.fit.server.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.server.model.Ticket;

public interface TicketRepository {
    boolean createTicket(Ticket ticket, EntityManager em);

    boolean updateTicket(Ticket ticket);

    Ticket findTicketById(String id);

    List<Ticket> findTicketsByCustomerIdCard(String idCard);

    List<Ticket> findTicketsForExchange(List<String> ticketIds, EntityManager em);

    Ticket findTicketByIdWithSchedule(EntityManager em, String ticketId);

    List<Ticket> findTicketsByIdsWithSchedule(EntityManager em, List<String> ticketIds);

    List<Ticket> findTicketsByCustomerIdCardWithStatus(EntityManager em, String idCard, TicketStatus status);

    boolean updateTicket(EntityManager em, Ticket ticket);

    boolean updateTickets(EntityManager em, List<Ticket> tickets);
}
