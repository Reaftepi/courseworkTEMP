package kpi.pavlenko.shvets.coursework.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "therapy_protocol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TherapyProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "therapy_protocol_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinical_protocol_id")
    private ClinicalProtocol clinicalProtocol;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "therapy_id")
    private Therapy therapy;

    @Column(name = "dosage")
    private String dosage;

    @Column(name = "frequency")
    private String frequency;

    @Column(name = "instructions")
    private String instructions;
}
