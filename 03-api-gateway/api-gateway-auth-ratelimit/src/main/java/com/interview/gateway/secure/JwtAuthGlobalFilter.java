package com.interview.gateway.secure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT auth at the edge.
 * Public: /auth/**, /actuator/**
 * Protected: /api/** requires Authorization: Bearer <jwt>
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    public static final String ATTR_USER = "authenticatedUser";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);

    private final JwtService jwtService;

    public JwtAuthGlobalFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return JsonErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Missing or invalid Authorization header. Use: Bearer <token>", null);
        }

        String token = header.substring(7).trim();
        try {
            Claims claims = jwtService.parseAndValidate(token);
            String user = claims.getSubject();
            exchange.getAttributes().put(ATTR_USER, user);
            log.info("JWT OK user={} path={}", user, path);
            return chain.filter(exchange);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT invalid path={} reason={}", path, ex.getMessage());
            return JsonErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Invalid or expired JWT", null);
        }
    }

    private boolean isPublic(String path) {
        return path.startsWith("/auth/") || path.startsWith("/actuator");
    }

    @Override
    public int getOrder() {
        return -100; // before rate limit
    }
}
