package com.interview.retry;

public record ChargeRequest(
        String orderId,
        String customerId,
        double amount
) {
}
