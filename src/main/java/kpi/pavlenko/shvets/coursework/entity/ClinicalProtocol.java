package kpi.pavlenko.shvets.coursework.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clinical_protocol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicalProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clinical_protocol_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "result")
    private String result;

    //TODO WRITE OneToMany

    @OneToMany(mappedBy = "clinicalProtocol", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiagnosisProtocol> diagnoses;

    @OneToMany(mappedBy = "clinicalProtocol", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TherapyProtocol> therapies;
}
