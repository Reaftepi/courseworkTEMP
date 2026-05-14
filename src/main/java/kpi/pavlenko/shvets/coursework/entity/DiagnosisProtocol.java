package kpi.pavlenko.shvets.coursework.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnosis_protocol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiagnosisProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diagnosis_protocol_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinical_protocol_id")
    private ClinicalProtocol clinicalProtocol;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "diagnosis_id")
    private Diagnosis diagnosis;

    @Column(name = "is_main")
    private boolean isMain;          //TODO CHECK BOOLEAN

    @Column(name = "set_date")
    private LocalDate setDate;              //TODO CHECK
}
