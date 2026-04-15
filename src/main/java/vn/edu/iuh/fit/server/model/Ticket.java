package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "tickets")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id ;
}
