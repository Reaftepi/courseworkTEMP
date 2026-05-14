package kpi.pavlenko.shvets.coursework.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cabinet")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cabinet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cabinet_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "number", unique = true, nullable = false)
    private int number;

    @Column(name = "capacity", nullable = false)
    private int capacity;

}
