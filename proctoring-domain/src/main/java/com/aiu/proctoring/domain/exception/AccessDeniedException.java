package com.aiu.proctoring.domain.exception;

/**
 * Thrown when a user attempts unauthorized action or access to restricted resource.
 */
public class AccessDeniedException extends DomainException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
