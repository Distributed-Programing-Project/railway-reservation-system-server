package vn.edu.iuh.fit.server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "seats")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Seat {
    @Id
    @GeneratedValue
    @UuidGenerator
    private String id ;
}
