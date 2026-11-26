package com.hospital.scheduling.core.gateway;

import com.hospital.scheduling.core.domain.patient.Patient;

import java.util.Optional;

/** Porta de persistência dos pacientes. */
public interface PatientGateway {

    Patient save(Patient patient);

    Optional<Patient> findById(Long id);

    boolean existsById(Long id);

    boolean existsByEmail(String email);
}
