package com.hospital.scheduling.infrastructure.persistence.patient;

import com.hospital.scheduling.core.domain.patient.Patient;
import com.hospital.scheduling.core.gateway.PatientGateway;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class PatientJpaGateway implements PatientGateway {

    private final PatientJpaRepository repository;

    public PatientJpaGateway(PatientJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * Na atualização, carrega a linha e copia os campos, em vez de montar uma entidade nova:
     * assim o {@code created_at} da linha é preservado.
     */
    @Override
    @Transactional
    public Patient save(Patient patient) {
        PatientEntity entity = patient.getId() == null
                ? new PatientEntity()
                : repository.findById(patient.getId()).orElseThrow(() ->
                        new IllegalStateException("Paciente " + patient.getId() + " não existe para ser atualizado."));
        entity.setName(patient.getName());
        entity.setEmail(patient.getEmail());
        entity.setPhone(patient.getPhone());
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findById(Long id) {
        return repository.findById(id).map(PatientJpaGateway::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    private static Patient toDomain(PatientEntity entity) {
        return Patient.restore(entity.getId(), entity.getName(), entity.getEmail(), entity.getPhone());
    }
}
