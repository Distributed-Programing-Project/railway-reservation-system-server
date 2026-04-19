package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.server.constant.StatusSchedule;


@Table(name = "schedules")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private StatusSchedule status;

    @ManyToOne
    @JoinColumn(name = "train_id")
    private Train train;

    @ManyToOne
    private Route route;
}