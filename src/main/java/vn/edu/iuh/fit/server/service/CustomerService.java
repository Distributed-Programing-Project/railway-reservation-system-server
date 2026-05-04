package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.dto.CustomerDTO;
import vn.edu.iuh.fit.common.dto.CustomerDeleteRequestDTO;
import vn.edu.iuh.fit.common.dto.CustomerHistoryRequestDTO;
import vn.edu.iuh.fit.common.dto.CustomerSearchDTO;
import vn.edu.iuh.fit.common.response.Response;

public interface CustomerService {
  Response searchCustomers(CustomerSearchDTO searchDTO);

  Response createCustomer(CustomerDTO customerDTO);

  Response updateCustomer(CustomerDTO customerDTO);

  Response deleteCustomer(CustomerDeleteRequestDTO requestDTO);

  Response getCustomerHistory(CustomerHistoryRequestDTO requestDTO);
}
