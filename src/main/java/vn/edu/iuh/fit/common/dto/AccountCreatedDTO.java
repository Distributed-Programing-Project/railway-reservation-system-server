package vn.edu.iuh.fit.common.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreatedDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
    private String temporaryPassword;
}
