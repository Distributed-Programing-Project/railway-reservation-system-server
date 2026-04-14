package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.UuidGenerator;

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
    @GeneratedValue
    @UuidGenerator
    private String id ;

    @ToString.Exclude
    @Column(name = "train_code")
    private String trainCode ;

    @ToString.Exclude
    @OneToMany(mappedBy = "train")
    private List<Schedule> schedules ;
}
