package com.interview.order.dto;

public record PaymentResponse(
        String paymentId,
        String orderId,
        String status,
        String message
) {
}
