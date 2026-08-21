package com.interview.gateway.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Downstream Order service — clients should reach this VIA API Gateway, not directly in production.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/{orderId}")
    public Map<String, Object> getOrder(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Gateway", required = false) String gateway) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "order-backend");
        body.put("port", 8101);
        body.put("orderId", orderId);
        body.put("status", "CONFIRMED");
        body.put("amount", 999.0);
        body.put("receivedRequestId", requestId);
        body.put("receivedFromGateway", gateway);
        body.put("message", "Order fetched from order-backend");
        return body;
    }

    @PostMapping
    public Map<String, Object> createOrder(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Gateway", required = false) String gateway) {

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "order-backend");
        body.put("port", 8101);
        body.put("orderId", orderId);
        body.put("status", "CREATED");
        body.put("request", request);
        body.put("receivedRequestId", requestId);
        body.put("receivedFromGateway", gateway);
        return body;
    }
}
