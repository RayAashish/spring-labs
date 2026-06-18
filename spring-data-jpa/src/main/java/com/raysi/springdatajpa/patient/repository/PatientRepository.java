package com.raysi.springdatajpa.patient.repository;

import com.raysi.springdatajpa.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientName(String name);
    Optional<Patient> findByPatientNameAndBirthDate(String name, LocalDate birthDate);
}
