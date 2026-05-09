package vn.edu.iuh.fit.server.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.dto.CustomerDTO;
import vn.edu.iuh.fit.common.dto.CustomerHistoryItemDTO;
import vn.edu.iuh.fit.server.model.Customer;

public interface CustomerRepository {
  List<CustomerDTO> searchActiveCustomers(EntityManager em, String keyword, int page, int size);

  long countActiveCustomers(EntityManager em, String keyword);

  boolean existsByIdCard(EntityManager em, String idCard, String excludeCustomerId);

  boolean existsByPassport(EntityManager em, String passport, String excludeCustomerId);

  boolean existsByEmail(EntityManager em, String email, String excludeCustomerId);

  Customer findCustomerById(EntityManager em, String customerId);

  Customer createCustomer(EntityManager em, Customer customer);

  Customer updateCustomer(EntityManager em, Customer customer);

  void deleteCustomer(EntityManager em, Customer customer);

  boolean hasAnyTicket(EntityManager em, String customerId);

  boolean hasAnyInvoice(EntityManager em, String customerId);

  boolean hasUpcomingPaidTicket(EntityManager em, String customerId, LocalDateTime now);

  List<CustomerHistoryItemDTO> findCustomerTicketHistory(EntityManager em, String customerId);

  double sumCustomerInvoiceTotalAmount(EntityManager em, String customerId);
}
