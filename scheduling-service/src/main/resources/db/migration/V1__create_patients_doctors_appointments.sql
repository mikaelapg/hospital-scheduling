-- Pacientes. O e-mail é único e já chega em minúsculas do domínio.
CREATE TABLE patients (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL,
    phone      VARCHAR(20),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uk_patients_email UNIQUE (email)
);

-- Médicos. O CRM fica no formato 123456/SP (no máximo 10 caracteres).
CREATE TABLE doctors (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    crm        VARCHAR(10)  NOT NULL,
    specialty  VARCHAR(80)  NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uk_doctors_crm UNIQUE (crm)
);

-- Consultas. Paciente e médico não podem ser apagados enquanto tiverem consultas:
-- o histórico do paciente não pode perder registros.
CREATE TABLE appointments (
    id             BIGSERIAL     PRIMARY KEY,
    patient_id     BIGINT        NOT NULL REFERENCES patients (id) ON DELETE RESTRICT,
    doctor_id      BIGINT        NOT NULL REFERENCES doctors (id) ON DELETE RESTRICT,
    scheduled_at   TIMESTAMP     NOT NULL,
    reason         VARCHAR(500),
    status         VARCHAR(20)   NOT NULL,
    clinical_notes VARCHAR(4000),
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    CONSTRAINT ck_appointments_status
        CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_appointments_notes_only_when_completed
        CHECK (clinical_notes IS NULL OR status = 'COMPLETED')
);

-- Um médico não pode ter duas consultas agendadas no mesmo horário. O índice é parcial:
-- consultas canceladas ou realizadas liberam o horário. Garantido no banco, e não só na
-- aplicação, para que duas requisições simultâneas não consigam marcar o mesmo horário.
CREATE UNIQUE INDEX uk_appointments_doctor_slot
    ON appointments (doctor_id, scheduled_at)
    WHERE status = 'SCHEDULED';

-- Histórico do paciente: consultas por paciente em ordem de data.
CREATE INDEX idx_appointments_patient_scheduled_at
    ON appointments (patient_id, scheduled_at);
