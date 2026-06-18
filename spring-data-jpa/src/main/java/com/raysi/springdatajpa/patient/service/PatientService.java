package com.raysi.springdatajpa.patient.service;

import com.raysi.springdatajpa.patient.entity.Patient;
import com.raysi.springdatajpa.patient.repository.PatientRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Transactional
    public Patient patientService(){
        Patient p1 = patientRepository.findById(103L).orElseThrow();
        Patient p2 = patientRepository.findById(103L).orElseThrow();

        p1.setPatientName("Durian");
        System.out.println(p1 == p2);
        return p1;
    }
}
