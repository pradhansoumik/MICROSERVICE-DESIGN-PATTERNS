package com.interview.saga.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates Payment Service.
 * Forward: charge customer
 * Compensate: refund (if payment had succeeded — in our happy path payment is last,
 * so payment failure usually means no refund needed; still shown for completeness)
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final Map<String, String> payments = new ConcurrentHashMap<>();

    @Value("${saga.failure.mode:NONE}")
    private volatile String failureMode;

    public String charge(String orderId, String customerId, double amount) {
        if ("PAYMENT".equalsIgnoreCase(failureMode)) {
            log.error("PAYMENT deliberately failing for order {}", orderId);
            throw new IllegalStateException("Payment declined for customer " + customerId);
        }
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payments.put(paymentId, "CAPTURED");
        log.info("PAYMENT captured {} order={} amount={}", paymentId, orderId, amount);
        return paymentId;
    }

    /** Compensation — refund if money was taken */
    public void refund(String paymentId) {
        if (paymentId == null) {
            return;
        }
        payments.put(paymentId, "REFUNDED");
        log.info("COMPENSATE: PAYMENT refunded {}", paymentId);
    }

    public void setFailureMode(String mode) {
        this.failureMode = mode.toUpperCase();
    }

    public String getFailureMode() {
        return failureMode;
    }

    public String statusOf(String paymentId) {
        return payments.getOrDefault(paymentId, "UNKNOWN");
    }
}
