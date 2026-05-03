package vn.edu.iuh.fit.client.session;

import vn.edu.iuh.fit.common.dto.AccountDTO;

public final class ClientSessionContext {

  private static final ClientSessionContext INSTANCE = new ClientSessionContext();

  private String accountId;
  private String username;
  private String employeeId;

  private ClientSessionContext() {
  }

  public static ClientSessionContext getInstance() {
    return INSTANCE;
  }

  public void setAccount(AccountDTO dto) {
    if (dto == null)
      return;
    this.accountId = dto.getId();
    this.username = dto.getUsername();

    this.employeeId = dto.getEmployeeId();

    // Fallback thực dụng: nếu server chưa trả employeeId,
    // dùng username như employee_code, ví dụ QL001.
    if (this.employeeId == null || this.employeeId.isBlank()) {
      this.employeeId = dto.getUsername();
    }
  }

  public String getAccountId() {
    return accountId;
  }

  public String getUsername() {
    return username;
  }

  public String getEmployeeId() {
    return employeeId;
  }
}
