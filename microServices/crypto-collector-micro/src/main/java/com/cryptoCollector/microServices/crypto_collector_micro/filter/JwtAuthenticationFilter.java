package com.cryptoCollector.microServices.crypto_collector_micro.filter;

import com.cryptoCollector.microServices.crypto_collector_micro.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean skip = path.startsWith("/actuator") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/api/public");  // Endpoints públicos para demos
        if (skip) {
            logger.info("⏭️  Saltando filtro JWT para ruta pública: {}", path);
        }
        return skip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        logger.info("🔐 JWT Filter - Path: {} - Auth Header Present: {}", request.getRequestURI(), authHeader != null);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            logger.info("🔑 Token extraído (primeros 20 chars): {}...",
                    token.substring(0, Math.min(20, token.length())));

            String subject = jwtUtil.getSubjectFromToken(token);
            logger.info("👤 Subject extraído del token: {}", subject);

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Crear autenticación con credenciales y marcar como autenticado
                var auth = new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.info("✅ Autenticación establecida para: {} - isAuthenticated: {}",
                        subject, auth.isAuthenticated());
            } else if (subject == null) {
                logger.warn("❌ No se pudo extraer subject del token - Token inválido");
            }
        }
        filterChain.doFilter(request, response);
    }
}
