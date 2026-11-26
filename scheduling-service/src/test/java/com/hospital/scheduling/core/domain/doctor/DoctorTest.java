package com.hospital.scheduling.core.domain.doctor;

import com.hospital.scheduling.core.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoctorTest {

    @ParameterizedTest
    @CsvSource({
            "123456/SP, 123456/SP",
            "123456/sp, 123456/SP",
            "' 1234 / rj ', 1234/RJ",
            "1234567-/MG, 1234567/MG"
    })
    void normalizaOCrm(String typed, String expected) {
        Doctor doctor = Doctor.create("Dr. João", typed, "Cardiologia");

        assertThat(doctor.getCrm()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123/SP", "12345678/SP", "123456", "123456/S", "123456/SPX", "abcdef/SP"})
    void recusaCrmForaDoFormato(String crm) {
        assertThatThrownBy(() -> Doctor.create("Dr. João", crm, "Cardiologia"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("CRM");
    }

    @Test
    void exigeCrmNomeEEspecialidade() {
        assertThatThrownBy(() -> Doctor.create("Dr. João", null, "Cardiologia"))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Doctor.create("", "123456/SP", "Cardiologia"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("nome");
        assertThatThrownBy(() -> Doctor.create("Dr. João", "123456/SP", " "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("especialidade");
    }

    @Test
    void atualizaSemMudarOCrm() {
        Doctor doctor = Doctor.restore(3L, "Dr. João", "123456/SP", "Clínica geral");

        Doctor updated = doctor.update("Dr. João Lima", "Cardiologia");

        assertThat(updated.getCrm()).isEqualTo("123456/SP");
        assertThat(updated.getSpecialty()).isEqualTo("Cardiologia");
        assertThat(updated).isEqualTo(doctor);
    }
}
