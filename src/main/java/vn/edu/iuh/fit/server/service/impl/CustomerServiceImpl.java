package vn.edu.iuh.fit.server.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.dto.CustomerDTO;
import vn.edu.iuh.fit.server.dto.CustomerDeleteRequestDTO;
import vn.edu.iuh.fit.server.dto.CustomerPageDTO;
import vn.edu.iuh.fit.server.dto.CustomerSearchDTO;
import vn.edu.iuh.fit.server.model.Customer;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.repository.CustomerRepository;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.impl.CustomerRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.service.CustomerService;
import vn.edu.iuh.fit.server.util.JPAUtils;
import vn.edu.iuh.fit.server.util.ValidationUtils;

import java.time.LocalDateTime;
import java.util.List;

public class CustomerServiceImpl implements CustomerService {

  private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

  private final CustomerRepository customerRepository = new CustomerRepositoryImpl();
  private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();

  @Override
  public Response searchCustomers(CustomerSearchDTO searchDTO) {
    CustomerSearchDTO request = searchDTO != null ? searchDTO : new CustomerSearchDTO();
    List<String> errors = ValidationUtils.validate(request);
    if (!errors.isEmpty()) {
      return Response.error(String.join(", ", errors));
    }

    EntityManager em = JPAUtils.getEntityManager();
    try {
      int page = request.getPage();
      int size = request.getSize();
      String keyword = request.getKeyword();

      List<CustomerDTO> customers = customerRepository.searchActiveCustomers(em, keyword, page, size);
      long totalElements = customerRepository.countActiveCustomers(em, keyword);
      int totalPages = (int) Math.ceil((double) totalElements / size);

      CustomerPageDTO pageDTO = CustomerPageDTO.builder()
          .customers(customers)
          .totalElements(totalElements)
          .totalPages(totalPages)
          .currentPage(page)
          .build();

      return Response.success("Tìm kiếm khách hàng thành công", pageDTO);
    } catch (Exception e) {
      log.error("Failed to search customers", e);
      return Response.error("Lỗi khi tìm kiếm khách hàng: " + e.getMessage());
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @Override
  public Response createCustomer(CustomerDTO customerDTO) {
    List<String> errors = ValidationUtils.validate(customerDTO);
    if (!errors.isEmpty()) {
      return Response.error("Dữ liệu không hợp lệ: " + String.join(", ", errors));
    }
    if (customerDTO.getCustomerId() != null && !customerDTO.getCustomerId().isBlank()) {
      return Response.error("Customer ID phải để trống khi thêm mới");
    }

    EntityManager em = JPAUtils.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      if (customerRepository.existsByIdCard(em, customerDTO.getIdCard(), null)) {
        return Response.error("Số CCCD này đã được đăng ký. Vui lòng kiểm tra lại");
      }
      if (customerDTO.getEmail() != null && !customerDTO.getEmail().isBlank()
          && customerRepository.existsByEmail(em, customerDTO.getEmail(), null)) {
        return Response.error("Email này đã được đăng ký. Vui lòng kiểm tra lại");
      }

      tx.begin();
      Customer customer = Customer.builder()
          .name(customerDTO.getFullName())
          .idCard(customerDTO.getIdCard())
          .phoneNumber(normalizeBlankToNull(customerDTO.getPhone()))
          .email(normalizeBlankToNull(customerDTO.getEmail()))
          .isActive(true)
          .build();

      customerRepository.createCustomer(em, customer);
      tx.commit();

      return Response.success("Thêm khách hàng thành công", toDto(customer));
    } catch (Exception e) {
      rollbackQuietly(tx);
      log.error("Failed to create customer: idCard={}", customerDTO.getIdCard(), e);
      return Response.error("Lỗi khi thêm khách hàng: " + e.getMessage());
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @Override
  public Response updateCustomer(CustomerDTO customerDTO) {
    List<String> errors = ValidationUtils.validate(customerDTO);
    if (!errors.isEmpty()) {
      return Response.error("Dữ liệu không hợp lệ: " + String.join(", ", errors));
    }
    if (customerDTO.getCustomerId() == null || customerDTO.getCustomerId().isBlank()) {
      return Response.error("Customer ID không được để trống khi cập nhật");
    }

    EntityManager em = JPAUtils.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      Customer existing = customerRepository.findCustomerById(em, customerDTO.getCustomerId());
      if (existing == null) {
        return Response.error("Không tìm thấy khách hàng: id=" + customerDTO.getCustomerId());
      }
      if (!existing.isActive()) {
        return Response.error("Không thể cập nhật khách hàng đã bị vô hiệu hóa: id=" + customerDTO.getCustomerId());
      }

      if (customerRepository.existsByIdCard(em, customerDTO.getIdCard(), existing.getId())) {
        return Response.error("Số CCCD này đã được đăng ký. Vui lòng kiểm tra lại");
      }
      if (customerDTO.getEmail() != null && !customerDTO.getEmail().isBlank()
          && customerRepository.existsByEmail(em, customerDTO.getEmail(), existing.getId())) {
        return Response.error("Email này đã được đăng ký. Vui lòng kiểm tra lại");
      }

      tx.begin();
      existing.setName(customerDTO.getFullName());
      existing.setIdCard(customerDTO.getIdCard());
      existing.setPhoneNumber(normalizeBlankToNull(customerDTO.getPhone()));
      existing.setEmail(normalizeBlankToNull(customerDTO.getEmail()));

      customerRepository.updateCustomer(em, existing);
      tx.commit();

      return Response.success("Cập nhật khách hàng thành công", toDto(existing));
    } catch (Exception e) {
      rollbackQuietly(tx);
      log.error("Failed to update customer: customerId={}", customerDTO.getCustomerId(), e);
      return Response.error("Lỗi khi cập nhật khách hàng: " + e.getMessage());
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @Override
  public Response deleteCustomer(CustomerDeleteRequestDTO requestDTO) {
    List<String> errors = ValidationUtils.validate(requestDTO);
    if (!errors.isEmpty()) {
      return Response.error("Dữ liệu không hợp lệ: " + String.join(", ", errors));
    }

    EntityManager em = JPAUtils.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      Employee requester = employeeRepository.findEmployeeById(em, requestDTO.getRequestEmployeeId());
      if (requester == null) {
        return Response.error("Không tìm thấy nhân viên: id=" + requestDTO.getRequestEmployeeId());
      }
      if (!Boolean.TRUE.equals(requester.getIsManager())) {
        return Response.error("Bạn không có quyền thực hiện thao tác này");
      }

      Customer customer = customerRepository.findCustomerById(em, requestDTO.getCustomerId());
      if (customer == null) {
        return Response.error("Không tìm thấy khách hàng: id=" + requestDTO.getCustomerId());
      }

      if (customerRepository.hasUpcomingPaidTicket(em, customer.getId(), LocalDateTime.now())) {
        return Response.error("Không thể vô hiệu hóa khách hàng đang có vé tàu sắp khởi hành");
      }

      boolean hasTicket = customerRepository.hasAnyTicket(em, customer.getId());
      boolean hasInvoice = customerRepository.hasAnyInvoice(em, customer.getId());

      tx.begin();
      if (hasTicket || hasInvoice) {
        customer.setActive(false);
        customerRepository.updateCustomer(em, customer);
        tx.commit();
        return Response.success("Xóa khách hàng thành công", toDto(customer));
      }

      customerRepository.deleteCustomer(em, customer);
      tx.commit();
      return Response.success("Xóa khách hàng thành công", requestDTO.getCustomerId());
    } catch (Exception e) {
      rollbackQuietly(tx);
      log.error("Failed to delete customer: customerId={}", requestDTO.getCustomerId(), e);
      return Response.error("Lỗi khi xóa khách hàng: " + e.getMessage());
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  private CustomerDTO toDto(Customer customer) {
    if (customer == null) {
      return null;
    }
    return CustomerDTO.builder()
        .customerId(customer.getId())
        .fullName(customer.getName())
        .idCard(customer.getIdCard())
        .phone(customer.getPhoneNumber())
        .email(customer.getEmail())
        .isActive(customer.isActive())
        .build();
  }

  private String normalizeBlankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void rollbackQuietly(EntityTransaction tx) {
    if (tx != null && tx.isActive()) {
      tx.rollback();
    }
  }
}
