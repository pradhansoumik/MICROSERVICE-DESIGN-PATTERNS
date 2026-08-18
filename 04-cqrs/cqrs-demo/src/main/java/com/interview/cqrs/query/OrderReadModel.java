package com.interview.cqrs.query;

/**
 * READ side — denormalized view optimized for queries (dashboard / customer app).
 * In production this might be a separate DB, Redis, Elasticsearch, etc.
 */
public record OrderReadModel(
        String orderId,
        String customerId,
        String productId,
        int quantity,
        double amount,
        String status,
        String displaySummary
) {
}
