package vn.edu.iuh.fit.server.repository.impl;

import java.util.List;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Ticket;
import vn.edu.iuh.fit.server.repository.TicketRepository;

public class TicketRepositoryImpl extends AbstractGenericRepositoryImpl<Ticket, String> implements TicketRepository {

  public TicketRepositoryImpl() {
    super(Ticket.class);
  }

  @Override
  public List<Ticket> findTicketsForExchange(List<String> ticketIds, EntityManager em) {
    // Dùng JOIN FETCH 2 cấp để lấy luôn thông tin chuyến tàu, tránh việc
    // lúc check 24h Hibernate lại bắn thêm query phụ.
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.schedule " +
        "WHERE t.id IN :ids";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("ids", ticketIds)
        .getResultList();
  }

  @Override
  public boolean createTicket(Ticket ticket, EntityManager em) {
    try {
      em.persist(ticket);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public boolean updateTicket(Ticket ticket) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'updateTicket'");
  }

  @Override
  public Ticket findTicketById(String id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findTicketById'");
  }

  @Override
  public List<Ticket> findTicketsByCustomerIdCard(String idCard) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findTicketsByCustomerIdCard'");
  }
}