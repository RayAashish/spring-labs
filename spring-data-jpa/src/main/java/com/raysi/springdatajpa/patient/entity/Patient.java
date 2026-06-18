package com.raysi.springdatajpa.patient.entity;

import com.raysi.springdatajpa.patient.entity.imp.BloodGroup;
import com.raysi.springdatajpa.patient.entity.imp.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CurrentTimestamp;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patientId;

    @Column(
            length = 32,
            nullable = false
    )
    private String patientName;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private BloodGroup bloodGroup;

    @CurrentTimestamp
    private LocalDateTime patientAdmittedTime;

    @Column(
          unique = true,
          nullable = false
    )
    private String email;
}
