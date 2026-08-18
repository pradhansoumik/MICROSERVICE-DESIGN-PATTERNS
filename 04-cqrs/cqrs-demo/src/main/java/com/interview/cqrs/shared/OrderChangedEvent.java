package com.interview.cqrs.shared;

/** Domain event published after a successful write — used to update read model. */
public record OrderChangedEvent(
        String orderId,
        String customerId,
        String productId,
        int quantity,
        double amount,
        String status
) {
}
