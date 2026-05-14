package kpi.pavlenko.shvets.coursework.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diagnosis")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diagnosis_protocol_id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;        //TODO CHECK code TYPE
}
