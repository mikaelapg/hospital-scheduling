package com.hospital.scheduling.infrastructure.persistence.doctor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorJpaRepository extends JpaRepository<DoctorEntity, Long> {

    boolean existsByCrm(String crm);
}
