package com.interview.saga;

import com.interview.saga.domain.PlaceOrderRequest;
import com.interview.saga.domain.SagaInstance;
import com.interview.saga.domain.SagaResponse;
import com.interview.saga.orchestrator.OrderSagaOrchestrator;
import com.interview.saga.service.InventoryService;
import com.interview.saga.service.OrderService;
import com.interview.saga.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/saga")
public class SagaController {

    private final OrderSagaOrchestrator orchestrator;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderService orderService;

    public SagaController(OrderSagaOrchestrator orchestrator,
                          InventoryService inventoryService,
                          PaymentService paymentService,
                          OrderService orderService) {
        this.orchestrator = orchestrator;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    /**
     * Place order through Saga orchestrator.
     */
    @PostMapping("/orders")
    public SagaResponse placeOrder(@RequestBody PlaceOrderRequest request) {
        SagaInstance saga = orchestrator.placeOrder(request);
        String message = switch (saga.getStatus()) {
            case COMPLETED -> "Order placed successfully via Saga";
            case COMPENSATED -> "Saga failed and compensated — system left consistent";
            default -> "Saga finished with status " + saga.getStatus();
        };
        return SagaResponse.from(saga, message);
    }

    @GetMapping("/{sagaId}")
    public SagaResponse getSaga(@PathVariable String sagaId) {
        SagaInstance saga = orchestrator.get(sagaId);
        if (saga == null) {
            return new SagaResponse(sagaId, null, null, null, null, "Not found", null, "Saga not found");
        }
        return SagaResponse.from(saga, "OK");
    }

    /**
     * NONE | INVENTORY | PAYMENT — flip without restart for live demos.
     */
    @PostMapping("/failure-mode")
    public Map<String, String> setFailureMode(@RequestParam String mode) {
        String normalized = mode.toUpperCase();
        inventoryService.setFailureMode(normalized);
        paymentService.setFailureMode(normalized);
        return Map.of(
                "failureMode", normalized,
                "hint", "NONE=happy path, INVENTORY=fail at reserve, PAYMENT=fail at charge (then compensate)"
        );
    }

    @GetMapping("/failure-mode")
    public Map<String, String> getFailureMode() {
        // Both are kept in sync by setFailureMode; show inventory's value
        return Map.of("failureMode", inventoryService.getFailureMode());
    }

    @GetMapping("/{sagaId}/snapshot")
    public Map<String, Object> snapshot(@PathVariable String sagaId) {
        SagaInstance saga = orchestrator.get(sagaId);
        if (saga == null) {
            return Map.of("error", "not found");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("saga", SagaResponse.from(saga, "snapshot"));
        body.put("orderStatus", orderService.statusOf(saga.getOrderId()));
        body.put("reservationStatus",
                saga.getReservationId() == null ? null : inventoryService.statusOf(saga.getReservationId()));
        body.put("paymentStatus",
                saga.getPaymentId() == null ? null : paymentService.statusOf(saga.getPaymentId()));
        return body;
    }
}
