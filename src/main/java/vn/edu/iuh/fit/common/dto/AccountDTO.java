package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private boolean active;
    private String employeeId;
    private boolean isManager;
}