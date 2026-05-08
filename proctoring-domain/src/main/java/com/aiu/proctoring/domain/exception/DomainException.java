package com.aiu.proctoring.domain.exception;

/**
 * Base domain exception for all business rule violations.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
