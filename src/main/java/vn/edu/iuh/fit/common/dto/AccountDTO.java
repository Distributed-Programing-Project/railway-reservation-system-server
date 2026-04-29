package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.util.List;

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
    private List<String> roleCodes;

    public boolean hasRole(String roleCode) {
        return roleCode != null
                && roleCodes != null
                && roleCodes.stream().anyMatch(roleCode::equalsIgnoreCase);
    }
}
