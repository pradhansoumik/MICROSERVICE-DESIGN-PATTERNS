package com.interview.gateway.secure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rate limit AFTER JWT auth, keyed by JWT subject (user).
 * Demo: in-memory fixed window. Production design → Redis (see DESIGN-RATE-LIMITING-SYSTEM.md).
 */
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitGlobalFilter.class);

    private final InMemoryRateLimiter rateLimiter;
    private final AppProperties props;

    public RateLimitGlobalFilter(InMemoryRateLimiter rateLimiter, AppProperties props) {
        this.rateLimiter = rateLimiter;
        this.props = props;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }

        String user = exchange.getAttribute(JwtAuthGlobalFilter.ATTR_USER);
        if (user == null || user.isBlank()) {
            // Auth filter should have blocked already; belt-and-suspenders
            return JsonErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "User not authenticated", null);
        }

        String key = "user:" + user;
        InMemoryRateLimiter.Decision decision = rateLimiter.check(key);

        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(decision.limit()));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, decision.remaining())));

        if (!decision.allowed()) {
            log.warn("RATE LIMIT user={} used={} limit={}", user, decision.used(), decision.limit());
            exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("limit", decision.limit());
            extra.put("windowSeconds", props.getRateLimit().getWindowSeconds());
            extra.put("user", user);
            return JsonErrorWriter.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Try again later.", extra);
        }

        log.info("RATE LIMIT allow user={} used={}/{}", user, decision.used(), decision.limit());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -90; // after JWT (-100)
    }
}
