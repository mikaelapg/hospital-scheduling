package com.hospital.scheduling.core.domain.appointment;

import com.hospital.scheduling.core.domain.exception.DomainValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Consulta de um paciente com um médico.
 * <p>
 * Imutável, como as entidades da Fase 2: toda alteração devolve uma nova instância, que
 * passa outra vez pelas validações.
 * <p>
 * Separa os dados de agenda (paciente, médico, data e motivo), que médicos e enfermeiros
 * podem alterar enquanto a consulta está agendada, das anotações clínicas, que só existem
 * depois que a consulta é realizada. Quem pode fazer cada operação é regra dos casos de uso.
 * <p>
 * O {@link Clock} entra como parâmetro nas operações que dependem de "agora", para que os
 * testes não dependam da hora em que rodam.
 */
public final class Appointment {

    public static final int REASON_MAX_LENGTH = 500;
    public static final int CLINICAL_NOTES_MAX_LENGTH = 4000;

    private final Long id;
    private final Long patientId;
    private final Long doctorId;
    private final LocalDateTime scheduledAt;
    private final String reason;
    private final AppointmentStatus status;
    private final String clinicalNotes;

    private Appointment(Long id, Long patientId, Long doctorId, LocalDateTime scheduledAt, String reason,
                        AppointmentStatus status, String clinicalNotes) {
        this.id = id;
        this.patientId = required(patientId, "O paciente da consulta é obrigatório.");
        this.doctorId = required(doctorId, "O médico da consulta é obrigatório.");
        this.scheduledAt = required(scheduledAt, "A data e hora da consulta são obrigatórias.")
                .truncatedTo(ChronoUnit.MINUTES);
        this.reason = optionalText(reason, "motivo", REASON_MAX_LENGTH);
        this.status = required(status, "A situação da consulta é obrigatória.");
        this.clinicalNotes = optionalText(clinicalNotes, "anotações clínicas", CLINICAL_NOTES_MAX_LENGTH);
        if (this.clinicalNotes != null && this.status != AppointmentStatus.COMPLETED) {
            throw new DomainValidationException("Só consultas realizadas têm anotações clínicas.");
        }
    }

    /**
     * Agenda uma consulta nova, ainda sem id.
     *
     * @throws DomainValidationException se a data não estiver no futuro
     */
    public static Appointment schedule(Long patientId, Long doctorId, LocalDateTime scheduledAt,
                                       String reason, Clock clock) {
        requireFuture(scheduledAt, clock);
        return new Appointment(null, patientId, doctorId, scheduledAt, reason, AppointmentStatus.SCHEDULED, null);
    }

    /** Reconstrói uma consulta que já existe, por exemplo ao ler do banco. */
    public static Appointment restore(Long id, Long patientId, Long doctorId, LocalDateTime scheduledAt,
                                      String reason, AppointmentStatus status, String clinicalNotes) {
        Objects.requireNonNull(id, "Uma consulta existente precisa de id.");
        return new Appointment(id, patientId, doctorId, scheduledAt, reason, status, clinicalNotes);
    }

    /**
     * Devolve uma cópia com os novos dados de agenda. O paciente não muda: remarcar não
     * transfere a consulta para outra pessoa.
     *
     * @throws DomainValidationException se a consulta não estiver agendada ou a nova data
     *                                   não estiver no futuro
     */
    public Appointment reschedule(Long newDoctorId, LocalDateTime newScheduledAt, String newReason, Clock clock) {
        requireStatus(AppointmentStatus.SCHEDULED, "Só consultas agendadas podem ser remarcadas.");
        requireFuture(newScheduledAt, clock);
        return new Appointment(id, patientId, newDoctorId, newScheduledAt, newReason, status, null);
    }

    /**
     * Registra que a consulta foi realizada, com as anotações do atendimento.
     *
     * @throws DomainValidationException se a consulta não estiver agendada, se o horário dela
     *                                   ainda não tiver chegado ou se faltarem as anotações
     */
    public Appointment complete(String notes, Clock clock) {
        requireStatus(AppointmentStatus.SCHEDULED, "Só consultas agendadas podem ser realizadas.");
        if (scheduledAt.isAfter(LocalDateTime.now(clock))) {
            throw new DomainValidationException("A consulta ainda não chegou ao horário marcado.");
        }
        requireText(notes, "As anotações clínicas são obrigatórias para concluir a consulta.");
        return new Appointment(id, patientId, doctorId, scheduledAt, reason, AppointmentStatus.COMPLETED, notes);
    }

    /**
     * Corrige ou complementa as anotações de uma consulta realizada: é a edição do histórico.
     *
     * @throws DomainValidationException se a consulta não tiver sido realizada
     */
    public Appointment updateClinicalNotes(String notes) {
        requireStatus(AppointmentStatus.COMPLETED, "Só consultas realizadas têm anotações clínicas.");
        requireText(notes, "As anotações clínicas não podem ficar vazias.");
        return new Appointment(id, patientId, doctorId, scheduledAt, reason, status, notes);
    }

    /**
     * Cancela a consulta.
     *
     * @throws DomainValidationException se a consulta não estiver agendada
     */
    public Appointment cancel() {
        requireStatus(AppointmentStatus.SCHEDULED, "Só consultas agendadas podem ser canceladas.");
        return new Appointment(id, patientId, doctorId, scheduledAt, reason, AppointmentStatus.CANCELLED, null);
    }

    /** Indica se a consulta ainda vai acontecer: agendada e com horário no futuro. */
    public boolean isUpcoming(Clock clock) {
        return status == AppointmentStatus.SCHEDULED && scheduledAt.isAfter(LocalDateTime.now(clock));
    }

    private void requireStatus(AppointmentStatus expected, String message) {
        if (status != expected) {
            throw new DomainValidationException(message);
        }
    }

    private static void requireFuture(LocalDateTime dateTime, Clock clock) {
        if (dateTime != null && !dateTime.isAfter(LocalDateTime.now(clock))) {
            throw new DomainValidationException("A consulta precisa ser marcada para uma data e hora futuras.");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(message);
        }
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new DomainValidationException(message);
        }
        return value;
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new DomainValidationException(
                    "O campo " + field + " deve ter no máximo " + maxLength + " caracteres.");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    /** Data e hora da consulta, sem segundos. */
    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    /** Motivo da consulta, ou {@code null} se não tiver sido informado. */
    public String getReason() {
        return reason;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    /** Anotações do atendimento; {@code null} enquanto a consulta não tiver sido realizada. */
    public String getClinicalNotes() {
        return clinicalNotes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
