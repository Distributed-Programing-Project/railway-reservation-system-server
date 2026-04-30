package vn.edu.iuh.fit.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import vn.edu.iuh.fit.common.constant.RoleCode;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@Builder
@Entity
@Table(name = "roles")
public class Role {

    public static final String ADMIN = RoleCode.ADMIN;
    public static final String STAFF = RoleCode.STAFF;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id", length = 36)
    private String id;

    @Column(name = "role_code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "role_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String name;
}
