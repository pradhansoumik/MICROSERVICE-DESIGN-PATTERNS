package com.interview.gateway.secure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

final class JsonErrorWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonErrorWriter() {
    }

    static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String error, String message,
                            Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("source", "api-gateway-auth-ratelimit");
        body.put("error", error);
        body.put("message", message);
        body.put("path", exchange.getRequest().getURI().getPath());
        body.put("timestamp", Instant.now().toString());
        if (extra != null) {
            body.putAll(extra);
        }

        byte[] bytes;
        try {
            bytes = MAPPER.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"success\":false,\"message\":\"error\"}".getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
