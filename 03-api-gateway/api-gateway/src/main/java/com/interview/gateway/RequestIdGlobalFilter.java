package com.interview.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter — runs for EVERY request through the gateway.
 * Interview talking point: cross-cutting concerns (correlation id, logging, auth later).
 */
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestIdGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        String finalRequestId = requestId;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Request-Id", finalRequestId)
                .build();

        log.info("GATEWAY incoming {} {} requestId={}",
                request.getMethod(), request.getURI().getPath(), finalRequestId);

        return chain.filter(exchange.mutate().request(request).build())
                .then(Mono.fromRunnable(() ->
                        log.info("GATEWAY completed {} requestId={}",
                                request.getURI().getPath(), finalRequestId)));
    }

    @Override
    public int getOrder() {
        return -1; // run early
    }
}
