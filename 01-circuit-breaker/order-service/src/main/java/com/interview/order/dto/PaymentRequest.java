package com.interview.order.dto;

public record PaymentRequest(
        String orderId,
        String customerId,
        double amount,
        String currency
) {
}
