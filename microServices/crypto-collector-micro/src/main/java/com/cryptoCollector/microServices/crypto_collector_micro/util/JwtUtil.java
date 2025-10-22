package com.cryptoCollector.microServices.crypto_collector_micro.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;

/**
 * Utilidad para validar tokens JWT en el microservicio crypto-collector.
 * Debe usar el mismo secreto que el servicio de autenticación.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:${JWT_SECRET:change_this_to_a_real_secret}}")
    private String jwtSecret;

    @Value("${jwt.expiration:${JWT_EXPIRATION:86400000}}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        try {
            if (secretBytes.length < 32) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                secretBytes = digest.digest(secretBytes);
            }
            return Keys.hmacShaKeyFor(secretBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct HMAC signing key", e);
        }
    }

    /**
     * Extrae el subject (username) del token JWT.
     * 
     * @param token Token JWT
     * @return Subject o null si el token es inválido
     */
    public String getSubjectFromToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody().getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Valida si el token JWT es válido.
     * 
     * @param token Token JWT
     * @return true si es válido, false en caso contrario
     */
    public boolean validateToken(String token) {
        return getSubjectFromToken(token) != null;
    }

    /**
     * Extrae el token del header Authorization.
     * 
     * @param authHeader Header Authorization (formato: "Bearer <token>")
     * @return Token JWT o null si el formato es inválido
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
