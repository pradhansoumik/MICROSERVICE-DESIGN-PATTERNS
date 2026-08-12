package com.interview.saga.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates Inventory Service.
 * Forward: reserve stock
 * Compensate: release reservation
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final Map<String, String> reservations = new ConcurrentHashMap<>();

    @Value("${saga.failure.mode:NONE}")
    private volatile String failureMode;

    public String reserve(String orderId, String productId, int quantity) {
        if ("INVENTORY".equalsIgnoreCase(failureMode)) {
            log.error("INVENTORY deliberately failing for order {}", orderId);
            throw new IllegalStateException("Inventory unavailable for product " + productId);
        }
        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        reservations.put(reservationId, "RESERVED");
        log.info("INVENTORY reserved {} for order {} product={} qty={}",
                reservationId, orderId, productId, quantity);
        return reservationId;
    }

    /** Compensation */
    public void release(String reservationId) {
        if (reservationId == null) {
            return;
        }
        reservations.put(reservationId, "RELEASED");
        log.info("COMPENSATE: INVENTORY released {}", reservationId);
    }

    public void setFailureMode(String mode) {
        this.failureMode = mode.toUpperCase();
    }

    public String getFailureMode() {
        return failureMode;
    }

    public String statusOf(String reservationId) {
        return reservations.getOrDefault(reservationId, "UNKNOWN");
    }
}
