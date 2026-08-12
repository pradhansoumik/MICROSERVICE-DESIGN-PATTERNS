package com.interview.saga.domain;

public record PlaceOrderRequest(
        String customerId,
        String productId,
        int quantity,
        double amount
) {
}
