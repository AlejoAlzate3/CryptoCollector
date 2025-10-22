package com.cryptoCollector.microServices.crypto_collector_micro.config;

import com.cryptoCollector.microServices.crypto_collector_micro.filter.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        logger.info("🔒 SecurityConfig inicializado con JwtAuthenticationFilter");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("🛡️ Configurando SecurityFilterChain...");
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/crypto/**").authenticated()
                .anyRequest().denyAll()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(context -> context.requireExplicitSave(false))  // Asegurar que el contexto se guarda automáticamente
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    logger.error("❌ Error de autenticación: {} - Path: {}", 
                        authException.getMessage(), request.getRequestURI());
                    response.sendError(401, "No autorizado");
                })
            );

        logger.info("✅ SecurityFilterChain configurado - Endpoints /api/crypto/** requieren autenticación JWT");
        return http.build();
    }
}
