package com.hospital.scheduling.infrastructure.persistence.patient;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientJpaRepository extends JpaRepository<PatientEntity, Long> {

    boolean existsByEmail(String email);
}
