package com.hospital.scheduling.core.domain.doctor;

import com.hospital.scheduling.core.domain.exception.DomainValidationException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Médico que atende as consultas.
 * <p>
 * O CRM é guardado no formato {@code 123456/SP}: número de 4 a 7 dígitos e a UF do conselho
 * regional. Espaços e hífens digitados são removidos e a UF fica em maiúsculas, para que o
 * mesmo CRM não apareça escrito de dois jeitos.
 */
public final class Doctor {

    public static final int NAME_MAX_LENGTH = 120;
    public static final int SPECIALTY_MAX_LENGTH = 80;

    private static final Pattern CRM = Pattern.compile("^\\d{4,7}/[A-Z]{2}$");

    private final Long id;
    private final String name;
    private final String crm;
    private final String specialty;

    private Doctor(Long id, String name, String crm, String specialty) {
        this.id = id;
        this.name = requiredText(name, "nome", NAME_MAX_LENGTH);
        this.crm = normalizedCrm(crm);
        this.specialty = requiredText(specialty, "especialidade", SPECIALTY_MAX_LENGTH);
    }

    /** Cadastra um médico novo, ainda sem id. */
    public static Doctor create(String name, String crm, String specialty) {
        return new Doctor(null, name, crm, specialty);
    }

    /** Reconstrói um médico que já existe, por exemplo ao ler do banco. */
    public static Doctor restore(Long id, String name, String crm, String specialty) {
        Objects.requireNonNull(id, "Um médico existente precisa de id.");
        return new Doctor(id, name, crm, specialty);
    }

    /** Devolve uma cópia com nome e especialidade atualizados. O CRM não muda. */
    public Doctor update(String newName, String newSpecialty) {
        return new Doctor(id, newName, crm, newSpecialty);
    }

    private static String normalizedCrm(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("O CRM do médico é obrigatório.");
        }
        String crm = value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (!CRM.matcher(crm).matches()) {
            throw new DomainValidationException("O CRM deve estar no formato 123456/SP.");
        }
        return crm;
    }

    private static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("O campo " + field + " do médico é obrigatório.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new DomainValidationException(
                    "O campo " + field + " do médico deve ter no máximo " + maxLength + " caracteres.");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** CRM no formato {@code 123456/SP}. */
    public String getCrm() {
        return crm;
    }

    public String getSpecialty() {
        return specialty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Doctor other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
