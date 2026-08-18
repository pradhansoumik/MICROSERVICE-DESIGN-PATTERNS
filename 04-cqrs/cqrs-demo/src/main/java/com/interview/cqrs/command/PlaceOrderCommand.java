package com.interview.cqrs.command;

public record PlaceOrderCommand(
        String customerId,
        String productId,
        int quantity,
        double amount
) {
}
