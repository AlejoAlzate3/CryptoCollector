package com.cryptoCollector.microServices.auth_microServices.exception;

/**
 * Excepción lanzada cuando hay un conflicto con datos existentes.
 * Por ejemplo: email duplicado, username duplicado, etc.
 */
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
