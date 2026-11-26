package com.hospital.scheduling.core.domain.patient;

import com.hospital.scheduling.core.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientTest {

    @Test
    void criaComNomeEEmailNormalizados() {
        Patient patient = Patient.create("  Ana Souza ", " Ana.Souza@Email.com ", null);

        assertThat(patient.getId()).isNull();
        assertThat(patient.getName()).isEqualTo("Ana Souza");
        assertThat(patient.getEmail()).isEqualTo("ana.souza@email.com");
        assertThat(patient.getPhone()).isNull();
    }

    @Test
    void exigeNome() {
        assertThatThrownBy(() -> Patient.create(" ", "ana@email.com", null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("nome");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ana", "ana@", "ana@email", "ana @email.com", "ana@@email.com"})
    void recusaEmailInvalido(String email) {
        assertThatThrownBy(() -> Patient.create("Ana", email, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void recusaNomeLongoDemais() {
        String name = "a".repeat(Patient.NAME_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Patient.create(name, "ana@email.com", null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void atualizaMantendoOId() {
        Patient patient = Patient.restore(7L, "Ana", "ana@email.com", null);

        Patient updated = patient.update("Ana Lima", "ana.lima@email.com", "11 99999-0000");

        assertThat(updated.getId()).isEqualTo(7L);
        assertThat(updated.getName()).isEqualTo("Ana Lima");
        assertThat(updated.getPhone()).isEqualTo("11 99999-0000");
        assertThat(updated).isEqualTo(patient);
    }
}
