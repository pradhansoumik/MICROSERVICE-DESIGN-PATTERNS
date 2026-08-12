package com.interview.saga.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates Order Service.
 * Forward: create order (PENDING)
 * Compensate: cancel order
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final Map<String, String> orders = new ConcurrentHashMap<>();

    public String createOrder(String customerId, String productId, int quantity, double amount) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        orders.put(orderId, "PENDING");
        log.info("ORDER created {} status=PENDING customer={}", orderId, customerId);
        return orderId;
    }

    public void confirmOrder(String orderId) {
        orders.put(orderId, "CONFIRMED");
        log.info("ORDER confirmed {}", orderId);
    }

    /** Compensation */
    public void cancelOrder(String orderId) {
        orders.put(orderId, "CANCELLED");
        log.info("COMPENSATE: ORDER cancelled {}", orderId);
    }

    public String statusOf(String orderId) {
        return orders.getOrDefault(orderId, "UNKNOWN");
    }
}
