package com.interview.order.controller;

import com.interview.order.dto.CreateOrderRequest;
import com.interview.order.dto.OrderResponse;
import com.interview.order.service.OrderService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public OrderController(OrderService orderService, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.orderService = orderService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostMapping
    public OrderResponse placeOrder(@RequestBody CreateOrderRequest request) {
        return orderService.placeOrder(request);
    }

    /**
     * Quick view of breaker state — great for live demo / interview screen share.
     * Also available via Actuator: GET /actuator/circuitbreakers
     */
    @GetMapping("/circuit-status")
    public Map<String, Object> circuitStatus() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("paymentService");
        CircuitBreaker.Metrics m = cb.getMetrics();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("name", cb.getName());
        status.put("state", cb.getState().name());
        status.put("failureRate", m.getFailureRate());
        status.put("slowCallRate", m.getSlowCallRate());
        status.put("numberOfFailedCalls", m.getNumberOfFailedCalls());
        status.put("numberOfSuccessfulCalls", m.getNumberOfSuccessfulCalls());
        status.put("numberOfNotPermittedCalls", m.getNumberOfNotPermittedCalls());
        return status;
    }
}
