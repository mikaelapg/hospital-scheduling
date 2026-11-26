package com.hospital.scheduling.infrastructure.persistence;

import com.hospital.scheduling.core.domain.appointment.Appointment;
import com.hospital.scheduling.core.domain.appointment.AppointmentStatus;
import com.hospital.scheduling.core.domain.doctor.Doctor;
import com.hospital.scheduling.core.domain.patient.Patient;
import com.hospital.scheduling.infrastructure.persistence.appointment.AppointmentJpaGateway;
import com.hospital.scheduling.infrastructure.persistence.doctor.DoctorJpaGateway;
import com.hospital.scheduling.infrastructure.persistence.patient.PatientJpaGateway;
import com.hospital.scheduling.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roda as migrations do Flyway num PostgreSQL de verdade (Testcontainers) e confere os
 * gateways contra ele. O {@code ddl-auto: validate} também falha aqui se o mapeamento JPA
 * não bater com as tabelas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestContainerConfig.class, PatientJpaGateway.class, DoctorJpaGateway.class,
        AppointmentJpaGateway.class})
class PersistenceGatewaysIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    /** Relógio bem no passado, para poder gravar consultas com datas já passadas. */
    private static final Clock PAST_CLOCK = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 10, 0);

    @Autowired
    private PatientJpaGateway patients;

    @Autowired
    private DoctorJpaGateway doctors;

    @Autowired
    private AppointmentJpaGateway appointments;

    private Patient ana;
    private Doctor joao;

    @BeforeEach
    void setUp() {
        ana = patients.save(Patient.create("Ana Souza", "ana@email.com", null));
        joao = doctors.save(Doctor.create("Dr. João", "123456/SP", "Cardiologia"));
    }

    private Appointment saveAt(Patient patient, LocalDateTime when) {
        return appointments.save(Appointment.schedule(patient.getId(), joao.getId(), when, null, PAST_CLOCK));
    }

    @Test
    void salvaELePacienteEMedico() {
        assertThat(ana.getId()).isNotNull();
        assertThat(patients.findById(ana.getId())).contains(ana);
        assertThat(patients.findById(ana.getId()).orElseThrow().getEmail()).isEqualTo("ana@email.com");
        assertThat(patients.existsByEmail("ana@email.com")).isTrue();
        assertThat(doctors.findById(joao.getId()).orElseThrow().getCrm()).isEqualTo("123456/SP");
        assertThat(doctors.existsByCrm("123456/SP")).isTrue();
    }

    @Test
    void atualizaPacienteNaMesmaLinha() {
        Patient updated = patients.save(ana.update("Ana Lima", "ana.lima@email.com", "11 99999-0000"));

        assertThat(updated.getId()).isEqualTo(ana.getId());
        assertThat(patients.findById(ana.getId()).orElseThrow().getName()).isEqualTo("Ana Lima");
    }

    @Test
    void emailDePacienteEUnico() {
        assertThatThrownBy(() -> patients.save(Patient.create("Outra Ana", "ANA@email.com", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void crmDeMedicoEUnico() {
        assertThatThrownBy(() -> doctors.save(Doctor.create("Dr. Outro", "123456/sp", "Pediatria")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listaAsConsultasDoPacienteEmOrdemDeData() {
        Appointment later = saveAt(ana, NOW.plusDays(5));
        Appointment earlier = saveAt(ana, NOW.minusDays(5));
        Patient bia = patients.save(Patient.create("Bia", "bia@email.com", null));
        saveAt(bia, NOW.plusDays(1));

        assertThat(appointments.findByPatientId(ana.getId())).containsExactly(earlier, later);
    }

    @Test
    void consultasFuturasIgnoramPassadasECanceladas() {
        saveAt(ana, NOW.minusDays(1));
        Appointment cancelled = saveAt(ana, NOW.plusDays(1));
        appointments.save(cancelled.cancel());
        Appointment soon = saveAt(ana, NOW.plusDays(2));
        Appointment later = saveAt(ana, NOW.plusDays(9));

        assertThat(appointments.findUpcomingByPatientId(ana.getId(), NOW)).containsExactly(soon, later);
    }

    @Test
    void detectaConflitoDeHorarioDoMedico() {
        Appointment existing = saveAt(ana, NOW.plusDays(1));

        assertThat(appointments.doctorHasScheduledAt(joao.getId(), NOW.plusDays(1), null)).isTrue();
        assertThat(appointments.doctorHasScheduledAt(joao.getId(), NOW.plusDays(1), existing.getId())).isFalse();
        assertThat(appointments.doctorHasScheduledAt(joao.getId(), NOW.plusDays(2), null)).isFalse();
    }

    @Test
    void horarioDeConsultaCanceladaFicaLivre() {
        Appointment cancelled = appointments.save(saveAt(ana, NOW.plusDays(1)).cancel());

        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointments.doctorHasScheduledAt(joao.getId(), NOW.plusDays(1), null)).isFalse();
        assertThat(saveAt(ana, NOW.plusDays(1)).getId()).isNotNull();
    }

    @Test
    void bancoRecusaDuasConsultasAgendadasNoMesmoHorarioDoMedico() {
        saveAt(ana, NOW.plusDays(1));
        Patient bia = patients.save(Patient.create("Bia", "bia@email.com", null));

        assertThatThrownBy(() -> saveAt(bia, NOW.plusDays(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardaAsAnotacoesDaConsultaRealizada() {
        Clock afterTheAppointment = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZONE);
        Appointment saved = saveAt(ana, NOW.minusDays(1));

        appointments.save(saved.complete("Pressão controlada.", afterTheAppointment));

        Appointment reloaded = appointments.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(reloaded.getClinicalNotes()).isEqualTo("Pressão controlada.");
    }
}
