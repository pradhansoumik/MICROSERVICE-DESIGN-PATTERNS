package com.interview.saga.domain;

import java.util.List;

public record SagaResponse(
        String sagaId,
        String orderId,
        SagaStatus status,
        String reservationId,
        String paymentId,
        String failureReason,
        List<String> timeline,
        String message
) {
    public static SagaResponse from(SagaInstance s, String message) {
        return new SagaResponse(
                s.getSagaId(),
                s.getOrderId(),
                s.getStatus(),
                s.getReservationId(),
                s.getPaymentId(),
                s.getFailureReason(),
                s.getTimeline(),
                message
        );
    }
}
