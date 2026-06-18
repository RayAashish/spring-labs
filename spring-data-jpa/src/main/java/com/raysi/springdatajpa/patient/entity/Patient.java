package com.raysi.springdatajpa.patient.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"patientName", "birthDate"})
)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long patientId;

    @Column(
            length = 32,
            nullable = false
    )
    private String patientName;

    private LocalDate birthDate;

    @Column(
            updatable = false
    )
    private LocalDateTime patientAdmittedTime;

    @Column(
          unique = true,
          nullable = false
    )
    private String email;
}
