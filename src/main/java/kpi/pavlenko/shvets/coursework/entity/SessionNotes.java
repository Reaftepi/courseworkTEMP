package kpi.pavlenko.shvets.coursework.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "content")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime created_at;
}
