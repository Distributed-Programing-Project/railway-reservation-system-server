package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.DocumentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleBuyerDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private String buyerName;
  private DocumentType documentType;
  private String documentNumber;
  private String buyerEmail;
  private String buyerPhone;
  private boolean hasAccount;
  private String customerId;
}

