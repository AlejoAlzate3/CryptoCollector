package com.cryptoCollector.microServices.crypto_collector_micro.exception;

/**
 * Excepción lanzada cuando hay un error al comunicarse con la API externa de CoinGecko.
 */
public class ExternalApiException extends RuntimeException {
    
    public ExternalApiException(String message) {
        super(message);
    }
    
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
