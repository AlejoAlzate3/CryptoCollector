package com.cryptoCollector.apiGateway.resources.filter;

import com.cryptoCollector.apiGateway.config.JwtConfig;
import com.cryptoCollector.apiGateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtConfig jwtConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Obtener token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            JwtUtil jwtUtil = new JwtUtil(jwtConfig.getSecret());
            if (!jwtUtil.validateToken(token)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            Claims claims = jwtUtil.extractAllClaims(token);

            exchange = exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.add("X-User-Id", claims.getSubject());
                        h.add("X-User-Email", String.valueOf(claims.get("email")));
                        h.add("X-User-Roles", String.valueOf(claims.get("roles")));
                    }))
                    .build();

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/public") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/auth/swagger-ui") ||
                path.startsWith("/crypto/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/auth/v3/api-docs") ||
                path.startsWith("/crypto/v3/api-docs") ||
                path.equals("/actuator/health");
    }
}
