package com.hospital.scheduling.infrastructure.persistence.appointment;

import com.hospital.scheduling.core.domain.appointment.Appointment;
import com.hospital.scheduling.core.domain.appointment.AppointmentStatus;
import com.hospital.scheduling.core.gateway.AppointmentGateway;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class AppointmentJpaGateway implements AppointmentGateway {

    private final AppointmentJpaRepository repository;

    public AppointmentJpaGateway(AppointmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Appointment save(Appointment appointment) {
        AppointmentEntity entity = appointment.getId() == null
                ? new AppointmentEntity()
                : repository.findById(appointment.getId()).orElseThrow(() ->
                        new IllegalStateException("Consulta " + appointment.getId() + " não existe para ser atualizada."));
        entity.setPatientId(appointment.getPatientId());
        entity.setDoctorId(appointment.getDoctorId());
        entity.setScheduledAt(appointment.getScheduledAt());
        entity.setReason(appointment.getReason());
        entity.setStatus(appointment.getStatus());
        entity.setClinicalNotes(appointment.getClinicalNotes());
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Appointment> findById(Long id) {
        return repository.findById(id).map(AppointmentJpaGateway::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByPatientId(Long patientId) {
        return repository.findByPatientIdOrderByScheduledAtAsc(patientId).stream()
                .map(AppointmentJpaGateway::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findUpcomingByPatientId(Long patientId, LocalDateTime now) {
        return repository.findByPatientIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
                        patientId, AppointmentStatus.SCHEDULED, now).stream()
                .map(AppointmentJpaGateway::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean doctorHasScheduledAt(Long doctorId, LocalDateTime scheduledAt, Long ignoredAppointmentId) {
        // "id <> null" não casa com nenhuma linha no SQL, por isso a consulta sem o filtro de id.
        return ignoredAppointmentId == null
                ? repository.existsByDoctorIdAndScheduledAtAndStatus(doctorId, scheduledAt, AppointmentStatus.SCHEDULED)
                : repository.existsByDoctorIdAndScheduledAtAndStatusAndIdNot(
                        doctorId, scheduledAt, AppointmentStatus.SCHEDULED, ignoredAppointmentId);
    }

    private static Appointment toDomain(AppointmentEntity entity) {
        return Appointment.restore(entity.getId(), entity.getPatientId(), entity.getDoctorId(),
                entity.getScheduledAt(), entity.getReason(), entity.getStatus(), entity.getClinicalNotes());
    }
}
