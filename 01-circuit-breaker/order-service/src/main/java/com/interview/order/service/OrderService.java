package com.interview.order.service;

import com.interview.order.client.PaymentClient;
import com.interview.order.dto.CreateOrderRequest;
import com.interview.order.dto.OrderResponse;
import com.interview.order.dto.PaymentRequest;
import com.interview.order.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Real-life flow (e-commerce):
 * 1. Customer places order
 * 2. Order service creates orderId
 * 3. Calls Payment service (protected by Circuit Breaker)
 * 4. If payment OK → CONFIRMED
 * 5. If breaker/fallback → PENDING_PAYMENT (not a hard failure to user)
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final PaymentClient paymentClient;

    public OrderService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    public OrderResponse placeOrder(CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Placing order {} for customer {}", orderId, request.customerId());

        PaymentRequest paymentRequest = new PaymentRequest(
                orderId,
                request.customerId(),
                request.amount(),
                "INR"
        );

        try {
            CompletableFuture<PaymentResponse> future = paymentClient.pay(paymentRequest);
            PaymentResponse payment = future.get(); // demo simplicity; production: reactive or async API

            boolean usedFallback = "PENDING".equalsIgnoreCase(payment.status());
            String orderStatus = usedFallback ? "PENDING_PAYMENT" : "CONFIRMED";

            return new OrderResponse(
                    orderId,
                    request.customerId(),
                    orderStatus,
                    payment.paymentId(),
                    payment.status(),
                    payment.message(),
                    usedFallback
            );
        } catch (Exception e) {
            log.error("Unexpected error placing order {}", orderId, e);
            return new OrderResponse(
                    orderId,
                    request.customerId(),
                    "FAILED",
                    null,
                    "ERROR",
                    e.getMessage(),
                    true
            );
        }
    }
}
