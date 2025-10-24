package com.cryptoCollector.microServices.crypto_collector_micro.exception;

/**
 * Excepción lanzada cuando no hay datos sincronizados en la base de datos.
 */
public class NoDataAvailableException extends RuntimeException {

    public NoDataAvailableException(String message) {
        super(message);
    }

    public NoDataAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
