package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.dto.CustomerDTO;
import vn.edu.iuh.fit.server.dto.CustomerDeleteRequestDTO;
import vn.edu.iuh.fit.server.dto.CustomerSearchDTO;

public interface CustomerService {
  Response searchCustomers(CustomerSearchDTO searchDTO);

  Response createCustomer(CustomerDTO customerDTO);

  Response updateCustomer(CustomerDTO customerDTO);

  Response deleteCustomer(CustomerDeleteRequestDTO requestDTO);
}

