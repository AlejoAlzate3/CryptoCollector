package com.cryptoCollector.microServices.crypto_collector_micro.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:${JWT_SECRET:change_this_to_a_real_secret}}")
    private String jwtSecret;

    @Value("${jwt.expiration:${JWT_EXPIRATION:86400000}}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        logger.debug("🔐 JWT Secret length: {}", jwtSecret.length());
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        try {
            if (secretBytes.length < 32) {
                logger.debug("🔄 Hashing secret to 32 bytes using SHA-256");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                secretBytes = digest.digest(secretBytes);
            }
            return Keys.hmacShaKeyFor(secretBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct HMAC signing key", e);
        }
    }

    public String getSubjectFromToken(String token) {
        try {
            logger.debug("🔍 Validando token JWT...");
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            String subject = jws.getBody().getSubject();
            logger.info("✅ Token válido - Subject: {}", subject);
            return subject;
        } catch (ExpiredJwtException ex) {
            logger.warn("❌ Token expirado: {}", ex.getMessage());
            return null;
        } catch (MalformedJwtException ex) {
            logger.warn("❌ Token malformado: {}", ex.getMessage());
            return null;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            logger.error("❌ Firma del token inválida - El secret no coincide: {}", ex.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            logger.error("❌ Error validando token: {}", ex.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        return getSubjectFromToken(token) != null;
    }

    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
