package com.cryptoCollector.microServices.auth_microServices.constants;

public final class ErrorMessages {

    private ErrorMessages() {
        throw new UnsupportedOperationException("Esta es una clase de constantes y no debe ser instanciada");
    }

    // Errores de autenticación
    public static final String EMAIL_ALREADY_EXISTS = "Usuario y contraseña inválidos";
    public static final String INVALID_CREDENTIALS = "Credenciales inválidas";
    public static final String USER_NOT_FOUND = "Usuario y contraseña inválidos";

    // Errores de validación
    public static final String INVALID_EMAIL_FORMAT = "El formato del email es inválido";
    public static final String WEAK_PASSWORD = "La contraseña debe tener al menos 8 caracteres";
    public static final String EMPTY_FIELD = "El campo '%s' no puede estar vacío";

    // Errores de JWT
    public static final String INVALID_TOKEN = "Token inválido o expirado";
    public static final String MISSING_TOKEN = "Token de autenticación requerido";
    public static final String TOKEN_GENERATION_ERROR = "Error al generar el token de autenticación";
}
