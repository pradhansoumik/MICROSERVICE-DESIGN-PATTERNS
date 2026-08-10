package com.interview.retry;

import java.time.Instant;

public record ChargeResponse(
        String paymentId,
        String orderId,
        double amount,
        String status,
        String idempotencyKey,
        boolean replayed,
        long totalChargesExecuted,
        Instant processedAt,
        String message
) {
}
