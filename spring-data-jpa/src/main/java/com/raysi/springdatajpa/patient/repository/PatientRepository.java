package com.raysi.springdatajpa.patient.repository;

import com.raysi.springdatajpa.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientName(String name);
    Optional<Patient> findByPatientNameAndBirthDate(String name, LocalDate birthDate);

    @Query("select p.bloodGroup, count(p) from Patient as p group by p.bloodGroup")
    List<Object[]> countByBloodGroup();

    @Query("select p.patientName from Patient as p")
    List<String> findAllNames();

    @Query(value = "select * from patient", nativeQuery = true)
    List<Patient> findAllPatients();
}
