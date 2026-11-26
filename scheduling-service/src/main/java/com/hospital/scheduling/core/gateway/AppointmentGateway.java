package com.hospital.scheduling.core.gateway;

import com.hospital.scheduling.core.domain.appointment.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Porta de persistência das consultas. */
public interface AppointmentGateway {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(Long id);

    /** Todas as consultas do paciente, da mais antiga para a mais recente. */
    List<Appointment> findByPatientId(Long patientId);

    /** Consultas agendadas do paciente com horário depois de {@code now}, da mais próxima para a mais distante. */
    List<Appointment> findUpcomingByPatientId(Long patientId, LocalDateTime now);

    /**
     * Indica se o médico já tem outra consulta agendada nesse horário.
     *
     * @param ignoredAppointmentId consulta que não conta no conflito (a própria consulta, ao
     *                             remarcar); {@code null} ao agendar uma nova
     */
    boolean doctorHasScheduledAt(Long doctorId, LocalDateTime scheduledAt, Long ignoredAppointmentId);
}
