package com.hospital.scheduling.core.domain.exception;

/** Uma regra de uma entidade de domínio foi violada. */
public class DomainValidationException extends RuntimeException {

    public DomainValidationException(String message) {
        super(message);
    }
}
