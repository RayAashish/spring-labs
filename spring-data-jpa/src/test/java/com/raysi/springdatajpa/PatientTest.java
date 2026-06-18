package com.raysi.springdatajpa;


import com.raysi.springdatajpa.patient.entity.Patient;
import com.raysi.springdatajpa.patient.repository.PatientRepository;
import com.raysi.springdatajpa.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class PatientTest {

    @Autowired
    private  PatientRepository patientRepository;
    @Autowired
    private PatientService patientService;

    @Test
    public void patientInsertionTest(){
        LocalDateTime localDateTime = LocalDateTime.now();
        Patient patient = Patient.builder()
                .patientName("Alex")
                .birthDate(LocalDate.ofEpochDay(2001- 3 - 1))
                .patientAdmittedTime(localDateTime)
                .email("alex@gmail.com")
                .build();
        Patient patient2 = Patient.builder()
                .patientName("Durian")
                .birthDate(LocalDate.ofEpochDay(2002- 3 - 1))
                .patientAdmittedTime(localDateTime)
                .email("durian@gmail.com")
                .build();
        patientRepository.saveAll(List.of(patient, patient2));
    }

    @Test
    public void getPatientTest(){
        List<Patient> patients = patientRepository.findAll();
        if (patients.isEmpty())
            System.out.println("No patient data available");
        for (var patient : patients)
            System.out.println(patient.toString());
    }

    @Test
    public void deletePatientTest(){
        patientRepository.deleteAll();
    }

    @Test
    public void deletePatientById(){
        Long id = 102L;
        patientRepository.deleteById(id);
    }

    @Test
    public void transactionTest(){
        patientService.patientService();
    }
}
