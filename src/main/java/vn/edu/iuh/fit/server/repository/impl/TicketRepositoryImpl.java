package vn.edu.iuh.fit.server.repository.impl;

import java.util.List;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.server.model.Ticket;
import vn.edu.iuh.fit.server.repository.TicketRepository;

public class TicketRepositoryImpl extends AbstractGenericRepositoryImpl<Ticket, String> implements TicketRepository {

  public TicketRepositoryImpl() {
    super(Ticket.class);
  }

  @Override
  public List<Ticket> findTicketsForExchange(List<String> ticketIds, EntityManager em) {
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.seat seat " +
        "JOIN FETCH seat.carriage carriage " +
        "JOIN FETCH sd.schedule s " +
        "LEFT JOIN FETCH s.train " +
        "LEFT JOIN FETCH s.route r " +
        "LEFT JOIN FETCH r.departureStation " +
        "LEFT JOIN FETCH r.destinationStation " +
        "JOIN FETCH t.customer c " +
        "WHERE t.id IN :ids " +
        "AND t.exchanged = false " +
        "AND c.isActive = true";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("ids", ticketIds)
        .getResultList();
  }

  @Override
  public boolean createTicket(Ticket ticket, EntityManager em) {
    em.persist(ticket);
    return true;
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
        "JOIN FETCH sd.seat seat " +
        "JOIN FETCH seat.carriage carriage " +
        "JOIN FETCH sd.schedule s " +
        "LEFT JOIN FETCH s.train " +
        "LEFT JOIN FETCH s.route r " +
        "LEFT JOIN FETCH r.departureStation " +
        "LEFT JOIN FETCH r.destinationStation " +
        "WHERE (c.id = :idCard OR c.idCard = :idCard OR c.passport = :idCard) AND t.status = :status";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("idCard", idCard)
        .setParameter("status", status)
        .getResultList();
  }

  @Override
  public List<Ticket> findTicketsByCustomerIdCardWithStatusForExchange(EntityManager em, String idCard,
      TicketStatus status) {
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.customer c " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.seat seat " +
        "JOIN FETCH seat.carriage carriage " +
        "JOIN FETCH sd.schedule s " +
        "LEFT JOIN FETCH s.train " +
        "LEFT JOIN FETCH s.route r " +
        "LEFT JOIN FETCH r.departureStation " +
        "LEFT JOIN FETCH r.destinationStation " +
        "WHERE (c.id = :idCard OR c.idCard = :idCard OR c.passport = :idCard) AND t.status = :status";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("idCard", idCard)
        .setParameter("status", status)
        .getResultList();
  }

  @Override
  public Ticket findTicketByIdOrQrWithSchedule(EntityManager em, String value) {
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.customer c " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.seat seat " +
        "JOIN FETCH seat.carriage carriage " +
        "JOIN FETCH sd.schedule s " +
        "LEFT JOIN FETCH s.train " +
        "LEFT JOIN FETCH s.route r " +
        "LEFT JOIN FETCH r.departureStation " +
        "LEFT JOIN FETCH r.destinationStation " +
        "WHERE t.id = :value OR t.qrCode = :value";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("value", value)
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  @Override
  public List<Ticket> findTicketsByPassengerDocumentWithStatus(EntityManager em, String document, TicketStatus status) {
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.customer c " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.seat seat " +
        "JOIN FETCH seat.carriage carriage " +
        "JOIN FETCH sd.schedule s " +
        "LEFT JOIN FETCH s.train " +
        "LEFT JOIN FETCH s.route r " +
        "LEFT JOIN FETCH r.departureStation " +
        "LEFT JOIN FETCH r.destinationStation " +
        "WHERE t.passengerIdCard = :document AND t.status = :status";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("document", document)
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

  @Override
  public Ticket findOriginalTicketForDisplay(String ticketId) {
    return readOnly(em -> em.createQuery(
        "SELECT t FROM Ticket t " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.schedule " +
        "JOIN FETCH sd.seat seat " +
        "JOIN FETCH seat.carriage car " +
        "JOIN FETCH car.train " +
        "JOIN FETCH sd.segmentDepartureStation " +
        "JOIN FETCH sd.segmentDestinationStation " +
        "WHERE t.id = :ticketId", Ticket.class)
        .setParameter("ticketId", ticketId)
        .getResultStream()
        .findFirst()
        .orElse(null));
  }

  @Override
  public Double findActualPaidAmountByTicketId(EntityManager em, String ticketId) {
    if (ticketId == null || ticketId.isBlank()) {
      return null;
    }

    String jpql = """
        SELECT d.subTotal
        FROM InvoiceDetail d
        JOIN d.invoice i
        JOIN d.ticket t
        WHERE t.id = :ticketId
          AND i.type = vn.edu.iuh.fit.common.constant.InvoiceType.SALE
        ORDER BY i.issueDate DESC
        """;

    return em.createQuery(jpql, Double.class)
        .setParameter("ticketId", ticketId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  @Override
  public List<Ticket> findTicketsByStatusWithSchedule(EntityManager em, TicketStatus status) {
    String jpql = "SELECT t FROM Ticket t " +
        "JOIN FETCH t.customer c " +
        "JOIN FETCH t.scheduleDetail sd " +
        "JOIN FETCH sd.seat seat " +
        "JOIN FETCH seat.carriage carriage " +
        "JOIN FETCH sd.schedule s " +
        "LEFT JOIN FETCH s.train " +
        "LEFT JOIN FETCH s.route r " +
        "LEFT JOIN FETCH r.departureStation " +
        "LEFT JOIN FETCH r.destinationStation " +
        "WHERE t.status = :status " +
        "ORDER BY s.departureTime ASC";

    return em.createQuery(jpql, Ticket.class)
        .setParameter("status", status)
        .getResultList();
  }
}
