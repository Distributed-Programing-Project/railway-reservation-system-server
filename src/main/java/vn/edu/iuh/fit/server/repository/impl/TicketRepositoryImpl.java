package vn.edu.iuh.fit.server.repository.impl;

import java.util.List;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.constant.TicketStatus;
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
    return doInTransaction(em -> {
      em.merge(ticket);
      return true;
    });
  }

  @Override
  public Ticket findTicketById(String id) {
    return doWithEntityManager(em -> findTicketByIdWithSchedule(em, id));
  }

  @Override
  public List<Ticket> findTicketsByCustomerIdCard(String idCard) {
    return doWithEntityManager(em -> findTicketsByCustomerIdCardWithStatus(em, idCard, TicketStatus.PAID));
  }

  @Override
  public Ticket findTicketByIdWithSchedule(EntityManager em, String ticketId) {
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.customer c " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.schedule s " +
        "WHERE t.id = :ticketId";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("ticketId", ticketId)
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  @Override
  public List<Ticket> findTicketsByIdsWithSchedule(EntityManager em, List<String> ticketIds) {
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.customer c " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.schedule s " +
        "WHERE t.id IN :ids";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("ids", ticketIds)
        .getResultList();
  }

  @Override
  public List<Ticket> findTicketsByCustomerIdCardWithStatus(EntityManager em, String idCard, TicketStatus status) {
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.customer c " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.schedule s " +
        "WHERE (c.idCard = :idCard OR c.passport = :idCard) AND t.status = :status";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("idCard", idCard)
        .setParameter("status", status)
        .getResultList();
  }

  @Override
  public boolean updateTicket(EntityManager em, Ticket ticket) {
    em.merge(ticket);
    return true;
  }

  @Override
  public boolean updateTickets(EntityManager em, List<Ticket> tickets) {
    if (tickets == null || tickets.isEmpty()) {
      return true;
    }
    for (Ticket ticket : tickets) {
      em.merge(ticket);
    }
    return true;
  }
}
