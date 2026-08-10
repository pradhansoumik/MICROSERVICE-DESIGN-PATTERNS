package com.interview.order.dto;

public record CreateOrderRequest(
        String customerId,
        String productId,
        int quantity,
        double amount
) {
}
