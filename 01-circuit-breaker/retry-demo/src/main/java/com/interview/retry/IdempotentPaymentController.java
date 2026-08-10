package com.interview.retry;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Dummy payment APIs to explain Idempotency-Key in interviews.
 */
@RestController
@RequestMapping("/api/payments")
public class IdempotentPaymentController {

    private final IdempotentPaymentService paymentService;

    public IdempotentPaymentController(IdempotentPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * SAFE: same Idempotency-Key → same result, charge once.
     * Header: Idempotency-Key: ORD-123
     */
    @PostMapping("/charge")
    public ChargeResponse charge(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ChargeRequest request) {

        try {
            return paymentService.chargeWithIdempotency(idempotencyKey, request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * UNSAFE: every POST creates a new charge (double-charge on retry).
     */
    @PostMapping("/charge-unsafe")
    public ChargeResponse chargeUnsafe(@RequestBody ChargeRequest request) {
        return paymentService.chargeWithoutIdempotency(request);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return paymentService.stats();
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        paymentService.reset();
        return paymentService.stats();
    }
}
