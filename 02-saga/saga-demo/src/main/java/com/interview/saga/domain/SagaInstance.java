package com.interview.saga.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory saga instance — what an interviewer expects you to track:
 * which steps succeeded, so compensation can undo them in reverse order.
 */
public class SagaInstance {

    private final String sagaId;
    private final String orderId;
    private final String customerId;
    private final String productId;
    private final int quantity;
    private final double amount;

    private SagaStatus status = SagaStatus.STARTED;
    private String reservationId;
    private String paymentId;
    private String failureReason;
    private final List<String> timeline = new ArrayList<>();

    public SagaInstance(String sagaId, String orderId, String customerId,
                        String productId, int quantity, double amount) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        timeline.add("SAGA_STARTED");
    }

    public void addEvent(String event) {
        timeline.add(event);
    }

    public String getSagaId() { return sagaId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getAmount() { return amount; }
    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public List<String> getTimeline() { return List.copyOf(timeline); }
}
