package com.hospital.scheduling.core.gateway;

import com.hospital.scheduling.core.domain.doctor.Doctor;

import java.util.Optional;

/** Porta de persistência dos médicos. */
public interface DoctorGateway {

    Doctor save(Doctor doctor);

    Optional<Doctor> findById(Long id);

    boolean existsById(Long id);

    boolean existsByCrm(String crm);
}
