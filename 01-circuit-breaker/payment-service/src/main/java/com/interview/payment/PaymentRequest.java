package com.interview.payment;

public record PaymentRequest(
        String orderId,
        String customerId,
        double amount,
        String currency
) {
}
