package vn.edu.iuh.fit.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPageDTO implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private List<CustomerDTO> customers;
  private long totalElements;
  private int totalPages;
  private int currentPage;
}

