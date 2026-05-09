package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.dto.CustomerDTO;
import vn.edu.iuh.fit.common.dto.CustomerDeleteRequestDTO;
import vn.edu.iuh.fit.common.dto.CustomerHistoryItemDTO;
import vn.edu.iuh.fit.common.dto.CustomerHistoryRequestDTO;
import vn.edu.iuh.fit.common.dto.CustomerHistoryResponseDTO;
import vn.edu.iuh.fit.common.dto.CustomerPageDTO;
import vn.edu.iuh.fit.common.dto.CustomerSearchDTO;
import vn.edu.iuh.fit.common.message.CustomerMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.mapper.CustomerMapper;
import vn.edu.iuh.fit.server.model.Customer;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.repository.CustomerRepository;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.CustomerRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.service.CustomerService;
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

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                int size = request.getSize();
                if (size <= 0) {
                    size = 20;
                }
                int page = request.getPage();
                if (page < 0) {
                    page = 0;
                }
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

                return Response.success(CustomerMessages.SEARCH_SUCCESS, pageDTO);
            });
        } catch (Exception e) {
            log.error("Failed to search customers", e);
            return Response.error(CustomerMessages.SEARCH_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response createCustomer(CustomerDTO customerDTO) {
        List<String> errors = ValidationUtils.validate(customerDTO);
        if (!errors.isEmpty()) {
            return Response.error(CustomerMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
        }
        if (customerDTO.getCustomerId() != null && !customerDTO.getCustomerId().isBlank()) {
            return Response.error(CustomerMessages.CUSTOMER_ID_MUST_BE_NULL);
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                if (customerRepository.existsByIdCard(em, customerDTO.getIdCard(), null)) {
                    throw new IllegalArgumentException(CustomerMessages.ID_CARD_DUPLICATE);
                }
                if (customerDTO.getPassport() != null && !customerDTO.getPassport().isBlank()
                        && customerRepository.existsByPassport(em, customerDTO.getPassport(), null)) {
                    throw new IllegalArgumentException(CustomerMessages.PASSPORT_DUPLICATE);
                }
                if (customerDTO.getEmail() != null && !customerDTO.getEmail().isBlank()
                        && customerRepository.existsByEmail(em, customerDTO.getEmail(), null)) {
                    throw new IllegalArgumentException(CustomerMessages.EMAIL_DUPLICATE);
                }

                Customer customer = CustomerMapper.INSTANCE.toEntity(customerDTO);
                customer.setActive(true);

                customerRepository.createCustomer(em, customer);

                return Response.success(CustomerMessages.CREATE_SUCCESS, CustomerMapper.INSTANCE.toDto(customer));
            });
        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create customer: idCard={}", customerDTO.getIdCard(), e);
            return Response.error(CustomerMessages.CREATE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response updateCustomer(CustomerDTO customerDTO) {
        List<String> errors = ValidationUtils.validate(customerDTO);
        if (!errors.isEmpty()) {
            return Response.error(CustomerMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
        }
        if (customerDTO.getCustomerId() == null || customerDTO.getCustomerId().isBlank()) {
            return Response.error(CustomerMessages.CUSTOMER_ID_REQUIRED);
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Customer existing = customerRepository.findCustomerById(em, customerDTO.getCustomerId());
                if (existing == null) {
                    throw new IllegalArgumentException(CustomerMessages.customerNotFound(customerDTO.getCustomerId()));
                }
                if (!existing.isActive()) {
                    throw new IllegalArgumentException(CustomerMessages.customerInactive(customerDTO.getCustomerId()));
                }

                if (customerRepository.existsByIdCard(em, customerDTO.getIdCard(), existing.getId())) {
                    throw new IllegalArgumentException(CustomerMessages.ID_CARD_DUPLICATE);
                }
                if (customerDTO.getPassport() != null && !customerDTO.getPassport().isBlank()
                        && customerRepository.existsByPassport(em, customerDTO.getPassport(), existing.getId())) {
                    throw new IllegalArgumentException(CustomerMessages.PASSPORT_DUPLICATE);
                }
                if (customerDTO.getEmail() != null && !customerDTO.getEmail().isBlank()
                        && customerRepository.existsByEmail(em, customerDTO.getEmail(), existing.getId())) {
                    throw new IllegalArgumentException(CustomerMessages.EMAIL_DUPLICATE);
                }

                existing.setName(customerDTO.getFullName());
                existing.setIdCard(customerDTO.getIdCard());
                existing.setPassport(normalizeBlankToNull(customerDTO.getPassport()));
                existing.setPhoneNumber(normalizeBlankToNull(customerDTO.getPhone()));
                existing.setEmail(normalizeBlankToNull(customerDTO.getEmail()));

                customerRepository.updateCustomer(em, existing);

                return Response.success(CustomerMessages.UPDATE_SUCCESS, CustomerMapper.INSTANCE.toDto(existing));
            });
        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update customer: customerId={}", customerDTO.getCustomerId(), e);
            return Response.error(CustomerMessages.UPDATE_FAILED_PREFIX + e.getMessage());
        }
    }

    @Override
    public Response deleteCustomer(CustomerDeleteRequestDTO requestDTO) {
        List<String> errors = ValidationUtils.validate(requestDTO);
        if (!errors.isEmpty()) {
            return Response.error(CustomerMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Employee requester = employeeRepository.findEmployeeById(em, requestDTO.getRequestEmployeeId());
                if (requester == null) {
                    return Response.error(CustomerMessages.requestEmployeeNotFound(requestDTO.getRequestEmployeeId()));
                }
                if (requester.getEmployeeStatus() != EmployeeStatus.ACTIVE) {
                    return Response.error(CustomerMessages.requestEmployeeInactive(requestDTO.getRequestEmployeeId()));
                }
                if (!Boolean.TRUE.equals(requester.getIsManager())) {
                    return Response.error(CustomerMessages.MANAGER_ONLY);
                }

                Customer customer = customerRepository.findCustomerById(em, requestDTO.getCustomerId());
                if (customer == null) {
                    return Response.error(CustomerMessages.customerNotFound(requestDTO.getCustomerId()));
                }

                if (customerRepository.hasUpcomingPaidTicket(em, customer.getId(), LocalDateTime.now())) {
                    return Response.error(CustomerMessages.CUSTOMER_HAS_UPCOMING_TICKET);
                }

                customer.setActive(false);
                customerRepository.updateCustomer(em, customer);
                return Response.success(CustomerMessages.DELETE_SUCCESS, CustomerMapper.INSTANCE.toDto(customer));
            });
        } catch (Exception e) {
            log.error("Failed to delete customer: customerId={}", requestDTO.getCustomerId(), e);
            return Response.error(CustomerMessages.DELETE_FAILED_PREFIX + e.getMessage());
        }
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public Response getCustomerHistory(CustomerHistoryRequestDTO requestDTO) {
        List<String> errors = ValidationUtils.validate(requestDTO);
        if (!errors.isEmpty()) {
            return Response.error(CustomerMessages.DATA_INVALID_PREFIX + String.join(", ", errors));
        }

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                String customerId = requestDTO.getCustomerId().trim();
                List<CustomerHistoryItemDTO> items = customerRepository.findCustomerTicketHistory(em, customerId);
                double totalAmount = customerRepository.sumCustomerInvoiceTotalAmount(em, customerId);

                CustomerHistoryResponseDTO dto = CustomerHistoryResponseDTO.builder()
                        .items(items)
                        .totalAmount(totalAmount)
                        .build();

                return Response.success("Lấy lịch sử mua vé thành công", dto);
            });
        } catch (Exception e) {
            log.error("Failed to get customer history: customerId={}", requestDTO.getCustomerId(), e);
            return Response.error("Lỗi khi lấy lịch sử mua vé: " + e.getMessage());
        }
    }
}
