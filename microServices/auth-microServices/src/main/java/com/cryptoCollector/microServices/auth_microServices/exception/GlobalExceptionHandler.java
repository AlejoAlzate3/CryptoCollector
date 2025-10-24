package com.cryptoCollector.microServices.auth_microServices.exception;

import com.cryptoCollector.microServices.auth_microServices.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                log.warn("Recurso no encontrado: {} - Path: {}", ex.getMessage(), request.getRequestURI());

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ErrorResponse> handleConflictException(
                        ConflictException ex,
                        HttpServletRequest request) {

                log.warn("Conflicto detectado: {} - Path: {}", ex.getMessage(), request.getRequestURI());

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        @ExceptionHandler({ InvalidCredentialsException.class, BadCredentialsException.class })
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(
                        RuntimeException ex,
                        HttpServletRequest request) {

                log.warn("Credenciales inválidas: {} - Path: {}", ex.getMessage(), request.getRequestURI());

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .message("Credenciales inválidas")
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(
                        AuthenticationException ex,
                        HttpServletRequest request) {

                log.warn("Error de autenticación: {} - Path: {}", ex.getMessage(), request.getRequestURI());

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .message("No autorizado")
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(
                        AccessDeniedException ex,
                        HttpServletRequest request) {

                log.warn("Acceso denegado: {} - Path: {}", ex.getMessage(), request.getRequestURI());

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.FORBIDDEN.value())
                                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                                .message("Acceso denegado")
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                log.warn("Errores de validación en: {} - Total errores: {}",
                                request.getRequestURI(), ex.getBindingResult().getErrorCount());

                List<ErrorResponse.FieldError> fieldErrors = new ArrayList<>();
                for (FieldError error : ex.getBindingResult().getFieldErrors()) {
                        ErrorResponse.FieldError fieldError = ErrorResponse.FieldError.builder()
                                        .field(error.getField())
                                        .message(error.getDefaultMessage())
                                        .rejectedValue(error.getRejectedValue())
                                        .build();
                        fieldErrors.add(fieldError);
                }

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message("Errores de validación en los campos")
                                .path(request.getRequestURI())
                                .fieldErrors(fieldErrors)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {

                log.warn("Tipo de argumento incorrecto: {} - Path: {}", ex.getMessage(), request.getRequestURI());

                String message = String.format("El parámetro '%s' debe ser de tipo %s",
                                ex.getName(),
                                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido");

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {

                log.error("Error interno del servidor: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .message("Error interno del servidor")
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
}
