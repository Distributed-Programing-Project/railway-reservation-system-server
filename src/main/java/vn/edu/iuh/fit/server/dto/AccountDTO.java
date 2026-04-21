package vn.edu.iuh.fit.server.dto;

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
    private String id;
    private String username;
    private boolean active;
}
