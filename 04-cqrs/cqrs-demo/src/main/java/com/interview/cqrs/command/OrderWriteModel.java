package com.interview.cqrs.command;

/**
 * WRITE side — normalized domain state (source of truth for changes).
 */
public class OrderWriteModel {

    private final String orderId;
    private final String customerId;
    private final String productId;
    private final int quantity;
    private final double amount;
    private String status;

    public OrderWriteModel(String orderId, String customerId, String productId,
                           int quantity, double amount, String status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
