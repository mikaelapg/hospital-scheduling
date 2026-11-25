package com.hospital.scheduling.core.domain.appointment;

/**
 * Situação de uma consulta.
 * <ul>
 *   <li>{@code SCHEDULED}: agendada; pode ser remarcada, cancelada ou realizada.</li>
 *   <li>{@code COMPLETED}: realizada; só as anotações clínicas ainda podem mudar.</li>
 *   <li>{@code CANCELLED}: cancelada; não muda mais.</li>
 * </ul>
 */
public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
