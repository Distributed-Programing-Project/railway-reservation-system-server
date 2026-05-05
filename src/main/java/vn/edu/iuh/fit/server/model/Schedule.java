package vn.edu.iuh.fit.server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import vn.edu.iuh.fit.common.constant.StatusSchedule;
import vn.edu.iuh.fit.server.util.id.annotation.GeneratedScheduleId;

import java.time.LocalDateTime;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"train", "route", "scheduleDetails"})
@Builder
@Entity
@Table(name = "schedules")
@BatchSize(size = 25)
public class Schedule {
    @Id
    @GeneratedScheduleId
    @Column(name = "schedule_id", length = 10)
    private String id;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    private StatusSchedule status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id")
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY)
    private Route route;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.REMOVE)
    private List<ScheduleDetail> scheduleDetails;
}
