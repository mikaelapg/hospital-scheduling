package com.hospital.scheduling.infrastructure.persistence.doctor;

import com.hospital.scheduling.core.domain.doctor.Doctor;
import com.hospital.scheduling.core.gateway.DoctorGateway;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class DoctorJpaGateway implements DoctorGateway {

    private final DoctorJpaRepository repository;

    public DoctorJpaGateway(DoctorJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Doctor save(Doctor doctor) {
        DoctorEntity entity = doctor.getId() == null
                ? new DoctorEntity()
                : repository.findById(doctor.getId()).orElseThrow(() ->
                        new IllegalStateException("Médico " + doctor.getId() + " não existe para ser atualizado."));
        entity.setName(doctor.getName());
        entity.setCrm(doctor.getCrm());
        entity.setSpecialty(doctor.getSpecialty());
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Doctor> findById(Long id) {
        return repository.findById(id).map(DoctorJpaGateway::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCrm(String crm) {
        return repository.existsByCrm(crm);
    }

    private static Doctor toDomain(DoctorEntity entity) {
        return Doctor.restore(entity.getId(), entity.getName(), entity.getCrm(), entity.getSpecialty());
    }
}
