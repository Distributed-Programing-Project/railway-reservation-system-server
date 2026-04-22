package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.fit.common.constant.StatusSchedule;

import java.time.LocalDateTime;
import java.util.List;


@Table(name = "schedules")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@Builder
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "schedule_id", length = 36)
    private String id;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    private StatusSchedule status;

    @ManyToOne
    @JoinColumn(name = "train_id")
    @ToString.Exclude
    private Train train;

    @ManyToOne
    @ToString.Exclude
    private Route route;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    private List<ScheduleDetail> scheduleDetails;
}