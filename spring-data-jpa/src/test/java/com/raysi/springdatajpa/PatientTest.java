package com.raysi.springdatajpa;


import com.raysi.springdatajpa.patient.entity.Patient;
import com.raysi.springdatajpa.patient.repository.PatientRepository;
import com.raysi.springdatajpa.patient.service.PatientService;
import org.hibernate.query.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    public void getPatientByIdTest(){
        Optional<Patient> patient = patientRepository.findById(10L);
        if (patient.isPresent())
            System.out.println(patient);
        else
            System.out.println("No patient with such id exists");
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

    @Test
    public void getPatientByNameTest(){
        Optional<Patient> patient = patientRepository.findByPatientName("Kiran Rai");
        if (patient.isPresent())
            System.out.println(patient);
        else
            System.out.println("No such patient exists");
    }
    @Test
    public void getPatientByNameAndBirthDateTest(){
        LocalDate birthDate = LocalDate.of(1998, 5, 14);
        Optional<Patient> patient = patientRepository.findByPatientNameAndBirthDate("Aarav Sharma", birthDate);
        if (patient.isPresent())
            System.out.println(patient);
        else
            System.out.println("No such patient exists");
    }

    //Testing JPQL
    @Test
    public void jpqlTest(){
        List<Object[]> bloodGroupCount = patientRepository.countByBloodGroup();
        for (Object[] objects : bloodGroupCount){
            System.out.println(objects[0] + "       " + objects[1]);
        }
    }

    @Test
    public void findAllName(){
        List<String> patientNames = patientRepository.findAllNames();
        System.out.println(patientNames);
    }

    @Test
    public void findAllPatientsTest(){
        List<Patient> patients = patientRepository.findAllPatients();
        System.out.println(patients);
    }

    @Test
    public void pageableTesting(){
        Slice<Patient> patientPage = patientRepository.findAll(PageRequest.of(0, 2));
        for (Patient p : patientPage){
            System.out.println(p);
        }
    }

    @Test
    public void individualSortTesting(){
        List<Patient> patients = patientRepository.findAll(Sort.by("birthDate").ascending());
        System.out.println(patients);
    }

    @Test
    public void pagingSortingTest(){
        Slice<Patient> patients = patientRepository.findAll(PageRequest.of(1, 4, Sort.by("patientName").ascending()));
        for (Patient p : patients){
            System.out.println(p);
        }
    }
}
