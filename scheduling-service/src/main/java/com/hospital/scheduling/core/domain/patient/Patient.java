package com.hospital.scheduling.core.domain.patient;

import com.hospital.scheduling.core.domain.exception.DomainValidationException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Paciente do hospital.
 * <p>
 * O e-mail é obrigatório porque é por ele que o serviço de notificações manda os lembretes
 * de consulta. É guardado em minúsculas para que a unicidade não dependa de maiúsculas.
 */
public final class Patient {

    public static final int NAME_MAX_LENGTH = 120;
    public static final int EMAIL_MAX_LENGTH = 160;
    public static final int PHONE_MAX_LENGTH = 20;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Long id;
    private final String name;
    private final String email;
    private final String phone;

    private Patient(Long id, String name, String email, String phone) {
        this.id = id;
        this.name = requiredText(name, "nome", NAME_MAX_LENGTH);
        this.email = validEmail(email);
        this.phone = optionalText(phone, "telefone", PHONE_MAX_LENGTH);
    }

    /** Cadastra um paciente novo, ainda sem id. */
    public static Patient create(String name, String email, String phone) {
        return new Patient(null, name, email, phone);
    }

    /** Reconstrói um paciente que já existe, por exemplo ao ler do banco. */
    public static Patient restore(Long id, String name, String email, String phone) {
        Objects.requireNonNull(id, "Um paciente existente precisa de id.");
        return new Patient(id, name, email, phone);
    }

    /** Devolve uma cópia com os dados de contato atualizados. */
    public Patient update(String newName, String newEmail, String newPhone) {
        return new Patient(id, newName, newEmail, newPhone);
    }

    private static String validEmail(String value) {
        String email = requiredText(value, "e-mail", EMAIL_MAX_LENGTH).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new DomainValidationException("O e-mail do paciente é inválido.");
        }
        return email;
    }

    private static String requiredText(String value, String field, int maxLength) {
        String text = optionalText(value, field, maxLength);
        if (text == null) {
            throw new DomainValidationException("O campo " + field + " do paciente é obrigatório.");
        }
        return text;
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new DomainValidationException(
                    "O campo " + field + " do paciente deve ter no máximo " + maxLength + " caracteres.");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** E-mail em minúsculas. */
    public String getEmail() {
        return email;
    }

    /** Telefone, ou {@code null} se não tiver sido informado. */
    public String getPhone() {
        return phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
