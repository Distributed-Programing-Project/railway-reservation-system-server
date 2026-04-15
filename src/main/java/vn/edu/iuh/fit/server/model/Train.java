package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "trains")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Train {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id ;

    @ToString.Exclude
    @Column(name = "train_code")
    private String trainCode ;

    @ToString.Exclude
    @OneToMany(mappedBy = "train")
    private List<Schedule> schedules ;
}
