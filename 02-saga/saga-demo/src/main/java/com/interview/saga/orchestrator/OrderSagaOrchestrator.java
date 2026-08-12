package com.interview.saga.orchestrator;

import com.interview.saga.domain.PlaceOrderRequest;
import com.interview.saga.domain.SagaInstance;
import com.interview.saga.domain.SagaStatus;
import com.interview.saga.service.InventoryService;
import com.interview.saga.service.OrderService;
import com.interview.saga.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ORCHESTRATION-based Saga.
 *
 * Central coordinator runs steps in order and, on failure,
 * runs compensations in REVERSE order for completed steps only.
 *
 * Happy path:
 *   1) Create Order
 *   2) Reserve Inventory
 *   3) Charge Payment
 *   4) Confirm Order
 *
 * If Payment fails after Inventory reserved:
 *   compensate → Release Inventory → Cancel Order
 */
@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final Map<String, SagaInstance> sagas = new ConcurrentHashMap<>();

    public OrderSagaOrchestrator(OrderService orderService,
                                 InventoryService inventoryService,
                                 PaymentService paymentService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    public SagaInstance placeOrder(PlaceOrderRequest request) {
        String sagaId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("==== SAGA {} START ====", sagaId);

        // Step 1 — Create Order
        String orderId = orderService.createOrder(
                request.customerId(), request.productId(), request.quantity(), request.amount());

        SagaInstance saga = new SagaInstance(
                sagaId, orderId, request.customerId(),
                request.productId(), request.quantity(), request.amount());
        saga.setStatus(SagaStatus.ORDER_CREATED);
        saga.addEvent("ORDER_CREATED:" + orderId);
        sagas.put(sagaId, saga);

        try {
            // Step 2 — Reserve Inventory
            String reservationId = inventoryService.reserve(orderId, request.productId(), request.quantity());
            saga.setReservationId(reservationId);
            saga.setStatus(SagaStatus.INVENTORY_RESERVED);
            saga.addEvent("INVENTORY_RESERVED:" + reservationId);

            // Step 3 — Charge Payment
            String paymentId = paymentService.charge(orderId, request.customerId(), request.amount());
            saga.setPaymentId(paymentId);
            saga.setStatus(SagaStatus.PAYMENT_COMPLETED);
            saga.addEvent("PAYMENT_COMPLETED:" + paymentId);

            // Step 4 — Confirm Order
            orderService.confirmOrder(orderId);
            saga.setStatus(SagaStatus.COMPLETED);
            saga.addEvent("ORDER_CONFIRMED");
            saga.addEvent("SAGA_COMPLETED");
            log.info("==== SAGA {} COMPLETED ====", sagaId);
            return saga;

        } catch (Exception ex) {
            log.error("SAGA {} failed at a step: {}", sagaId, ex.getMessage());
            saga.setFailureReason(ex.getMessage());
            saga.setStatus(SagaStatus.COMPENSATING);
            saga.addEvent("STEP_FAILED:" + ex.getMessage());
            compensate(saga);
            return saga;
        }
    }

    /**
     * Compensate ONLY completed forward steps, in reverse order.
     * Interview tip: never compensate a step that never succeeded.
     */
    private void compensate(SagaInstance saga) {
        log.warn("==== SAGA {} COMPENSATING ====", saga.getSagaId());

        // Reverse of payment (only if payment id exists — usually null if payment failed)
        if (saga.getPaymentId() != null) {
            paymentService.refund(saga.getPaymentId());
            saga.addEvent("COMPENSATE_PAYMENT_REFUND:" + saga.getPaymentId());
        }

        // Reverse of inventory
        if (saga.getReservationId() != null) {
            inventoryService.release(saga.getReservationId());
            saga.addEvent("COMPENSATE_INVENTORY_RELEASE:" + saga.getReservationId());
        }

        // Reverse of order create
        orderService.cancelOrder(saga.getOrderId());
        saga.addEvent("COMPENSATE_ORDER_CANCEL:" + saga.getOrderId());

        saga.setStatus(SagaStatus.COMPENSATED);
        saga.addEvent("SAGA_COMPENSATED");
        log.warn("==== SAGA {} COMPENSATED ====", saga.getSagaId());
    }

    public SagaInstance get(String sagaId) {
        return sagas.get(sagaId);
    }

    public Map<String, SagaInstance> all() {
        return Map.copyOf(sagas);
    }
}
