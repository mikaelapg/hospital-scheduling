package com.hospital.scheduling.core.domain.appointment;

import com.hospital.scheduling.core.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    /** "Agora" nos testes: 25/11/2026, 10:00 em São Paulo. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T13:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final LocalDateTime TOMORROW = NOW.plusDays(1);

    private static Appointment scheduled() {
        return Appointment.restore(1L, 10L, 20L, TOMORROW, "Retorno", AppointmentStatus.SCHEDULED, null);
    }

    private static Appointment pastScheduled() {
        return Appointment.restore(1L, 10L, 20L, NOW.minusHours(1), "Retorno", AppointmentStatus.SCHEDULED, null);
    }

    @Nested
    class Schedule {

        @Test
        void agendaNoFuturoComoAgendada() {
            Appointment appointment = Appointment.schedule(10L, 20L, TOMORROW, "Dor de cabeça", CLOCK);

            assertThat(appointment.getId()).isNull();
            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
            assertThat(appointment.getPatientId()).isEqualTo(10L);
            assertThat(appointment.getDoctorId()).isEqualTo(20L);
            assertThat(appointment.getReason()).isEqualTo("Dor de cabeça");
            assertThat(appointment.getClinicalNotes()).isNull();
        }

        @Test
        void descartaOsSegundosDoHorario() {
            Appointment appointment = Appointment.schedule(10L, 20L, TOMORROW.withSecond(42), null, CLOCK);

            assertThat(appointment.getScheduledAt().getSecond()).isZero();
        }

        @Test
        void motivoEmBrancoViraNulo() {
            Appointment appointment = Appointment.schedule(10L, 20L, TOMORROW, "   ", CLOCK);

            assertThat(appointment.getReason()).isNull();
        }

        @Test
        void recusaDataNoPassadoOuAgora() {
            assertThatThrownBy(() -> Appointment.schedule(10L, 20L, NOW.minusMinutes(1), null, CLOCK))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("futuras");
            assertThatThrownBy(() -> Appointment.schedule(10L, 20L, NOW, null, CLOCK))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void exigePacienteMedicoEData() {
            assertThatThrownBy(() -> Appointment.schedule(null, 20L, TOMORROW, null, CLOCK))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("paciente");
            assertThatThrownBy(() -> Appointment.schedule(10L, null, TOMORROW, null, CLOCK))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("médico");
            assertThatThrownBy(() -> Appointment.schedule(10L, 20L, null, null, CLOCK))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("data");
        }

        @Test
        void recusaMotivoLongoDemais() {
            String reason = "a".repeat(Appointment.REASON_MAX_LENGTH + 1);

            assertThatThrownBy(() -> Appointment.schedule(10L, 20L, TOMORROW, reason, CLOCK))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("motivo");
        }
    }

    @Nested
    class Reschedule {

        @Test
        void trocaMedicoDataEMotivoMantendoOPaciente() {
            Appointment updated = scheduled().reschedule(30L, TOMORROW.plusDays(2), "Exames", CLOCK);

            assertThat(updated.getId()).isEqualTo(1L);
            assertThat(updated.getPatientId()).isEqualTo(10L);
            assertThat(updated.getDoctorId()).isEqualTo(30L);
            assertThat(updated.getScheduledAt()).isEqualTo(TOMORROW.plusDays(2));
            assertThat(updated.getReason()).isEqualTo("Exames");
            assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        }

        @Test
        void naoAlteraAInstanciaOriginal() {
            Appointment original = scheduled();

            original.reschedule(30L, TOMORROW.plusDays(2), null, CLOCK);

            assertThat(original.getDoctorId()).isEqualTo(20L);
            assertThat(original.getScheduledAt()).isEqualTo(TOMORROW);
        }

        @Test
        void recusaNovaDataNoPassado() {
            assertThatThrownBy(() -> scheduled().reschedule(20L, NOW.minusDays(1), null, CLOCK))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void recusaConsultaCanceladaOuRealizada() {
            Appointment cancelled = scheduled().cancel();
            Appointment completed = pastScheduled().complete("Paciente estável.", CLOCK);

            assertThatThrownBy(() -> cancelled.reschedule(20L, TOMORROW, null, CLOCK))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("agendadas");
            assertThatThrownBy(() -> completed.reschedule(20L, TOMORROW, null, CLOCK))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Complete {

        @Test
        void registraAsAnotacoes() {
            Appointment completed = pastScheduled().complete("  Pressão controlada.  ", CLOCK);

            assertThat(completed.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            assertThat(completed.getClinicalNotes()).isEqualTo("Pressão controlada.");
        }

        @Test
        void recusaConsultaQueAindaNaoChegouAoHorario() {
            assertThatThrownBy(() -> scheduled().complete("Anotações", CLOCK))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("horário");
        }

        @Test
        void exigeAnotacoes() {
            assertThatThrownBy(() -> pastScheduled().complete(" ", CLOCK))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("obrigatórias");
        }

        @Test
        void recusaConsultaCancelada() {
            Appointment cancelled = pastScheduled().cancel();

            assertThatThrownBy(() -> cancelled.complete("Anotações", CLOCK))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class ClinicalNotes {

        @Test
        void editaAsAnotacoesDeUmaConsultaRealizada() {
            Appointment completed = pastScheduled().complete("Primeira versão.", CLOCK);

            Appointment edited = completed.updateClinicalNotes("Versão corrigida.");

            assertThat(edited.getClinicalNotes()).isEqualTo("Versão corrigida.");
            assertThat(edited.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        }

        @Test
        void recusaConsultaNaoRealizada() {
            assertThatThrownBy(() -> scheduled().updateClinicalNotes("Anotações"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("realizadas");
        }

        @Test
        void recusaAnotacoesVazias() {
            Appointment completed = pastScheduled().complete("Primeira versão.", CLOCK);

            assertThatThrownBy(() -> completed.updateClinicalNotes(""))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        void naoAceitaAnotacoesEmConsultaQueNaoFoiRealizada() {
            assertThatThrownBy(() -> Appointment.restore(1L, 10L, 20L, TOMORROW, null,
                    AppointmentStatus.SCHEDULED, "Anotações"))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Upcoming {

        @Test
        void consultaAgendadaNoFuturoEstaPorVir() {
            assertThat(scheduled().isUpcoming(CLOCK)).isTrue();
        }

        @Test
        void consultaPassadaOuCanceladaNaoEstaPorVir() {
            assertThat(pastScheduled().isUpcoming(CLOCK)).isFalse();
            assertThat(scheduled().cancel().isUpcoming(CLOCK)).isFalse();
        }
    }

    @Test
    void igualdadePeloId() {
        Appointment a = scheduled();
        Appointment b = a.reschedule(30L, TOMORROW.plusDays(1), null, CLOCK);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(Appointment.schedule(10L, 20L, TOMORROW, null, CLOCK))
                .isNotEqualTo(Appointment.schedule(10L, 20L, TOMORROW, null, CLOCK));
    }
}
