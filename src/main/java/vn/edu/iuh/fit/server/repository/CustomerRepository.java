package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.dto.CustomerDTO;
import vn.edu.iuh.fit.server.model.Customer;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerRepository {
  List<CustomerDTO> searchActiveCustomers(EntityManager em, String keyword, int page, int size);

  long countActiveCustomers(EntityManager em, String keyword);

  boolean existsByIdCard(EntityManager em, String idCard, String excludeCustomerId);

  boolean existsByEmail(EntityManager em, String email, String excludeCustomerId);

  Customer findCustomerById(EntityManager em, String customerId);

  Customer createCustomer(EntityManager em, Customer customer);

  Customer updateCustomer(EntityManager em, Customer customer);

  void deleteCustomer(EntityManager em, Customer customer);

  boolean hasAnyTicket(EntityManager em, String customerId);

  boolean hasAnyInvoice(EntityManager em, String customerId);

  boolean hasUpcomingPaidTicket(EntityManager em, String customerId, LocalDateTime now);
}

