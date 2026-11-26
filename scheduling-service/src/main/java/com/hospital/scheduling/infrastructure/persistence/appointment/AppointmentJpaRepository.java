package com.hospital.scheduling.infrastructure.persistence.appointment;

import com.hospital.scheduling.core.domain.appointment.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, Long> {

    List<AppointmentEntity> findByPatientIdOrderByScheduledAtAsc(Long patientId);

    List<AppointmentEntity> findByPatientIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
            Long patientId, AppointmentStatus status, LocalDateTime after);

    boolean existsByDoctorIdAndScheduledAtAndStatus(
            Long doctorId, LocalDateTime scheduledAt, AppointmentStatus status);

    boolean existsByDoctorIdAndScheduledAtAndStatusAndIdNot(
            Long doctorId, LocalDateTime scheduledAt, AppointmentStatus status, Long id);
}
