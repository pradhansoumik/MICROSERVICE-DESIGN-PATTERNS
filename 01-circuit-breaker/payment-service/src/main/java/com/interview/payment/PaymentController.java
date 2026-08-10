package com.interview.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates a flaky payment gateway (Razorpay/Stripe-like).
 * Change failure mode via application.properties or runtime endpoint
 * to demonstrate Circuit Breaker OPEN / HALF_OPEN behavior.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Value("${payment.failure.mode:NONE}")
    private volatile String failureMode;

    @Value("${payment.slow.delay-ms:3000}")
    private long slowDelayMs;

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest request) throws InterruptedException {
        int n = callCount.incrementAndGet();
        log.info("Payment call #{} | mode={} | orderId={} | amount={}",
                n, failureMode, request.orderId(), request.amount());

        switch (failureMode.toUpperCase()) {
            case "FAIL" -> {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new PaymentResponse(null, request.orderId(), "FAILED",
                                "Payment gateway unavailable"));
            }
            case "SLOW" -> {
                Thread.sleep(slowDelayMs);
                return success(request);
            }
            case "ALTERNATE" -> {
                // Fail odd calls → useful to demo HALF_OPEN probe success/failure
                if (n % 2 == 1) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(new PaymentResponse(null, request.orderId(), "FAILED",
                                    "Intermittent gateway error"));
                }
                return success(request);
            }
            default -> {
                return success(request);
            }
        }
    }

    /**
     * Runtime switch so you can flip modes without restart while demoing.
     * Example: POST /api/payments/failure-mode?mode=FAIL
     */
    @PostMapping("/failure-mode")
    public Map<String, String> setFailureMode(@RequestParam String mode) {
        this.failureMode = mode.toUpperCase();
        callCount.set(0);
        log.warn("Failure mode changed to {}", this.failureMode);
        return Map.of("failureMode", this.failureMode);
    }

    @GetMapping("/failure-mode")
    public Map<String, Object> getFailureMode() {
        return Map.of(
                "failureMode", failureMode,
                "callCount", callCount.get(),
                "slowDelayMs", slowDelayMs
        );
    }

    private ResponseEntity<PaymentResponse> success(PaymentRequest request) {
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return ResponseEntity.ok(new PaymentResponse(
                paymentId,
                request.orderId(),
                "SUCCESS",
                "Payment captured successfully"
        ));
    }
}
