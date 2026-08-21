package com.interview.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catches failures from the filter chain / downstream services
 * and returns a structured JSON error to the client.
 *
 * Not Circuit Breaker — just error handling on the Gateway proxy path.
 */
@Component
public class StructuredErrorGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(StructuredErrorGlobalFilter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(ex -> writeStructuredError(exchange, ex));
    }

    private Mono<Void> writeStructuredError(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getURI().getPath();
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");

        log.error("GATEWAY structured error path={} requestId={} cause={}",
                path, requestId, ex.toString());

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("source", "api-gateway");
        body.put("path", path);
        body.put("requestId", requestId);
        body.put("message", "Downstream service failed or is unreachable");
        body.put("cause", ex.getClass().getSimpleName());
        body.put("timestamp", Instant.now().toString());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\"Downstream service failed\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // After RequestIdGlobalFilter (-1): that filter sets X-Request-Id on the exchange first
        return 0;
    }
}
