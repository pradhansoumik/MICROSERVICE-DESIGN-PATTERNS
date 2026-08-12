package com.interview.saga.domain;

public enum SagaStatus {
    STARTED,
    ORDER_CREATED,
    INVENTORY_RESERVED,
    PAYMENT_COMPLETED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
