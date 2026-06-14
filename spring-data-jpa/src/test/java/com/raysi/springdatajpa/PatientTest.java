package com.raysi.springdatajpa;


import com.raysi.springdatajpa.patient.entity.Patient;
import com.raysi.springdatajpa.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
public class PatientTest {

    @Autowired
    private  PatientRepository patientRepository;

    @Test
    public void patientInsertionTest(){
        Patient patient = Patient.builder()
                .patientName("Alex")
                .birthDate(LocalDate.ofEpochDay(2001- 7 - 2))
                .email("alex@gmail.com")
                .build();
        patientRepository.save(patient);
    }
}
