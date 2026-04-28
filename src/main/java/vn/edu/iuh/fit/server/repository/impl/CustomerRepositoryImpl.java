package vn.edu.iuh.fit.server.repository.impl;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import vn.edu.iuh.fit.common.constant.TicketStatus;
import vn.edu.iuh.fit.common.dto.CustomerDTO;
import vn.edu.iuh.fit.common.dto.CustomerHistoryItemDTO;
import vn.edu.iuh.fit.server.model.Customer;
import vn.edu.iuh.fit.server.repository.CustomerRepository;

public class CustomerRepositoryImpl implements CustomerRepository {

  @Override
  public List<CustomerDTO> searchActiveCustomers(EntityManager em, String keyword, int page, int size) {
    boolean hasKeyword = keyword != null && !keyword.isBlank();
    StringBuilder jpql = new StringBuilder()
        .append("SELECT new vn.edu.iuh.fit.common.dto.CustomerDTO(")
        .append("c.id, c.name, c.idCard, c.passport, c.phoneNumber, c.email, c.isActive, c.rewardPoints")
        .append(") ")
        .append("FROM Customer c ")
        .append("WHERE c.isActive = true ");

    if (hasKeyword) {
      jpql.append("AND (")
          .append("LOWER(c.name) LIKE :kw ")
          .append("OR LOWER(c.idCard) LIKE :kw ")
          .append("OR LOWER(c.passport) LIKE :kw ")
          .append("OR LOWER(c.phoneNumber) LIKE :kw ")
          .append("OR LOWER(c.email) LIKE :kw")
          .append(") ");
    }
    jpql.append("ORDER BY c.name ASC, c.id ASC");

    TypedQuery<CustomerDTO> query = em.createQuery(jpql.toString(), CustomerDTO.class)
        .setFirstResult(page * size)
        .setMaxResults(size);

    if (hasKeyword) {
      query.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
    }
    return query.getResultList();
  }

  @Override
  public long countActiveCustomers(EntityManager em, String keyword) {
    boolean hasKeyword = keyword != null && !keyword.isBlank();
    StringBuilder jpql = new StringBuilder()
        .append("SELECT COUNT(c) ")
        .append("FROM Customer c ")
        .append("WHERE c.isActive = true ");

    if (hasKeyword) {
      jpql.append("AND (")
          .append("LOWER(c.name) LIKE :kw ")
          .append("OR LOWER(c.idCard) LIKE :kw ")
          .append("OR LOWER(c.passport) LIKE :kw ")
          .append("OR LOWER(c.phoneNumber) LIKE :kw ")
          .append("OR LOWER(c.email) LIKE :kw")
          .append(") ");
    }

    TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
    if (hasKeyword) {
      query.setParameter("kw", "%" + keyword.trim().toLowerCase() + "%");
    }
    Long total = query.getSingleResult();
    return total != null ? total : 0L;
  }

  @Override
  public boolean existsByIdCard(EntityManager em, String idCard, String excludeCustomerId) {
    if (idCard == null || idCard.isBlank()) {
      return false;
    }
    boolean hasExclude = excludeCustomerId != null && !excludeCustomerId.isBlank();
    StringBuilder jpql = new StringBuilder("SELECT COUNT(c) FROM Customer c WHERE c.idCard = :idCard ");
    if (hasExclude) {
      jpql.append("AND c.id <> :excludeId ");
    }
    TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
        .setParameter("idCard", idCard.trim());
    if (hasExclude) {
      query.setParameter("excludeId", excludeCustomerId.trim());
    }
    Long count = query.getSingleResult();
    return count != null && count > 0;
  }

  @Override
  public boolean existsByEmail(EntityManager em, String email, String excludeCustomerId) {
    if (email == null || email.isBlank()) {
      return false;
    }
    boolean hasExclude = excludeCustomerId != null && !excludeCustomerId.isBlank();
    StringBuilder jpql = new StringBuilder("SELECT COUNT(c) FROM Customer c WHERE c.email = :email ");
    if (hasExclude) {
      jpql.append("AND c.id <> :excludeId ");
    }
    TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
        .setParameter("email", email.trim());
    if (hasExclude) {
      query.setParameter("excludeId", excludeCustomerId.trim());
    }
    Long count = query.getSingleResult();
    return count != null && count > 0;
  }

  @Override
  public Customer findCustomerById(EntityManager em, String customerId) {
    if (customerId == null || customerId.isBlank()) {
      return null;
    }
    return em.find(Customer.class, customerId.trim());
  }

  @Override
  public Customer createCustomer(EntityManager em, Customer customer) {
    em.persist(customer);
    return customer;
  }

  @Override
  public Customer updateCustomer(EntityManager em, Customer customer) {
    return em.merge(customer);
  }

  @Override
  public void deleteCustomer(EntityManager em, Customer customer) {
    if (customer == null) {
      return;
    }
    Customer managed = em.contains(customer) ? customer : em.merge(customer);
    em.remove(managed);
  }

  @Override
  public boolean hasAnyTicket(EntityManager em, String customerId) {
    if (customerId == null || customerId.isBlank()) {
      return false;
    }
    String jpql = "SELECT COUNT(t) FROM Ticket t WHERE t.customer.id = :customerId";
    Long count = em.createQuery(jpql, Long.class)
        .setParameter("customerId", customerId.trim())
        .getSingleResult();
    return count != null && count > 0;
  }

  @Override
  public boolean hasAnyInvoice(EntityManager em, String customerId) {
    if (customerId == null || customerId.isBlank()) {
      return false;
    }
    String jpql = "SELECT COUNT(i) FROM Invoice i WHERE i.customer.id = :customerId";
    Long count = em.createQuery(jpql, Long.class)
        .setParameter("customerId", customerId.trim())
        .getSingleResult();
    return count != null && count > 0;
  }

    @Override
    public boolean hasUpcomingPaidTicket(EntityManager em, String customerId, LocalDateTime now) {
        if (customerId == null || customerId.isBlank()) {
            return false;
        }
        if (now == null) {
            now = LocalDateTime.now();
        }
        String jpql = "SELECT COUNT(t) FROM Ticket t " +
            "JOIN t.scheduleDetail sd " +
            "JOIN sd.schedule s " +
            "WHERE t.customer.id = :customerId " +
            "AND t.status = :status " +
            "AND s.departureTime > :now " +
            "AND t.originalTicketId IS NULL";
        Long count = em.createQuery(jpql, Long.class)
            .setParameter("customerId", customerId.trim())
            .setParameter("status", TicketStatus.PAID)
            .setParameter("now", now)
            .getSingleResult();
        return count != null && count > 0;
    }

  @Override
  public List<CustomerHistoryItemDTO> findCustomerTicketHistory(EntityManager em, String customerId) {
    if (customerId == null || customerId.isBlank()) {
      return List.of();
    }

    String jpql = "SELECT new vn.edu.iuh.fit.common.dto.CustomerHistoryItemDTO("
        + "i.issueDate, "
        + "t.id, "
        + "tr.trainCode, "
        + "dep.name, "
        + "des.name, "
        + "s.departureTime, "
        + "s.arrivalTime, "
        + "car.number, "
        + "seat.type, "
        + "seat.number, "
        + "(COALESCE(d.subTotal, 0.0) - d.discount + d.insurance)"
        + ") "
        + "FROM InvoiceDetail d "
        + "JOIN d.invoice i "
        + "JOIN d.ticket t "
        + "LEFT JOIN t.scheduleDetail sd "
        + "LEFT JOIN sd.schedule s "
        + "LEFT JOIN s.train tr "
        + "LEFT JOIN s.route r "
        + "LEFT JOIN r.departureStation dep "
        + "LEFT JOIN r.destinationStation des "
        + "LEFT JOIN sd.seat seat "
        + "LEFT JOIN seat.carriage car "
        + "WHERE i.customer.id = :customerId "
        + "ORDER BY i.issueDate DESC, t.id DESC";

    return em.createQuery(jpql, CustomerHistoryItemDTO.class)
        .setParameter("customerId", customerId.trim())
        .getResultList();
  }

  @Override
  public double sumCustomerInvoiceTotalAmount(EntityManager em, String customerId) {
    if (customerId == null || customerId.isBlank()) {
      return 0.0;
    }

    String jpql = "SELECT COALESCE(SUM(i.totalAmount), 0.0) FROM Invoice i WHERE i.customer.id = :customerId";
    Double total = em.createQuery(jpql, Double.class)
        .setParameter("customerId", customerId.trim())
        .getSingleResult();
    return total != null ? total : 0.0;
  }
}
