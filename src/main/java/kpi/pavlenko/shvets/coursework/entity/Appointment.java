package kpi.pavlenko.shvets.coursework.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cabinet_id")
    private Cabinet cabinet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinical_protocol_id")
    private ClinicalProtocol clinicalProtocol;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "duration")
    private int duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppStatus status;
}
