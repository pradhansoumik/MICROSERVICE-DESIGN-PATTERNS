package com.interview.order.dto;

public record OrderResponse(
        String orderId,
        String customerId,
        String status,
        String paymentId,
        String paymentStatus,
        String message,
        boolean usedFallback
) {
}
