package kpi.pavlenko.shvets.coursework.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "sex", nullable = false)
    private String sex;

    @Column(name = "height", nullable = false)
    private double height;

    @Column(name = "weight", nullable = false)
    private double weight;

    @Column(name = "status", nullable = false)
    private String status;                          //TODO ASK ABOUT status TYPE

    @Column(name = "behavior")                     //TODO ASK ABOUT nullable
    private String behavior;

    @Column(name = "date_of_arrival", nullable = false)
    private LocalDate dateOfArrival;

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
    //TODO WRITE OneToMany

}
