package com.interview.payment;

public record PaymentResponse(
        String paymentId,
        String orderId,
        String status,
        String message
) {
}
