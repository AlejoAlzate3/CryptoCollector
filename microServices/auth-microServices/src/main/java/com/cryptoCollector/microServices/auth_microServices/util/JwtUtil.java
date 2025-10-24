package com.cryptoCollector.microServices.auth_microServices.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Date;

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
            throw new IllegalStateException("Falló al construir la clave de firma HMAC", e);
        }
    }

    public String generateToken(String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

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

    public boolean validateToken(String token) {
        return getSubjectFromToken(token) != null;
    }
}