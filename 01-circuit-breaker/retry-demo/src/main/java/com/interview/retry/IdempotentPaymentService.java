package com.interview.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dummy payment API demonstrating Idempotency-Key.
 *
 * Real-life: client retries after timeout. Without idempotency → double charge.
 * With same Idempotency-Key → return stored result, charge only once.
 */
@Service
public class IdempotentPaymentService {

    private static final Logger log = LoggerFactory.getLogger(IdempotentPaymentService.class);

    /** key → first successful response */
    private final ConcurrentHashMap<String, ChargeResponse> store = new ConcurrentHashMap<>();

    /** How many times money was actually charged (real side-effect) */
    private final AtomicLong chargesExecuted = new AtomicLong(0);

    /** How many HTTP calls received (including replays) */
    private final AtomicLong httpCallsReceived = new AtomicLong(0);

    public ChargeResponse chargeWithIdempotency(String idempotencyKey, ChargeRequest request) {
        httpCallsReceived.incrementAndGet();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        // Already processed this key? → replay, NO second charge
        ChargeResponse existing = store.get(idempotencyKey);
        if (existing != null) {
            log.info("IDEMPOTENT REPLAY for key={} — returning stored paymentId={} (no new charge)",
                    idempotencyKey, existing.paymentId());
            return new ChargeResponse(
                    existing.paymentId(),
                    existing.orderId(),
                    existing.amount(),
                    existing.status(),
                    idempotencyKey,
                    true,
                    chargesExecuted.get(),
                    existing.processedAt(),
                    "Replayed stored result — customer NOT charged again"
            );
        }

        // First time for this key → perform real charge
        long chargeNo = chargesExecuted.incrementAndGet();
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant now = Instant.now();

        ChargeResponse created = new ChargeResponse(
                paymentId,
                request.orderId(),
                request.amount(),
                "SUCCESS",
                idempotencyKey,
                false,
                chargeNo,
                now,
                "Payment charged successfully (first time for this key)"
        );

        store.put(idempotencyKey, created);
        log.info("NEW CHARGE #{} for key={} paymentId={} amount={}",
                chargeNo, idempotencyKey, paymentId, request.amount());
        return created;
    }

    /**
     * Unsafe version — every call charges again (shows the problem Retry can cause).
     */
    public ChargeResponse chargeWithoutIdempotency(ChargeRequest request) {
        httpCallsReceived.incrementAndGet();
        long chargeNo = chargesExecuted.incrementAndGet();
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.warn("UNSAFE CHARGE #{} (no idempotency) paymentId={} amount={}",
                chargeNo, paymentId, request.amount());

        return new ChargeResponse(
                paymentId,
                request.orderId(),
                request.amount(),
                "SUCCESS",
                null,
                false,
                chargeNo,
                Instant.now(),
                "Charged again — duplicate risk if client retries"
        );
    }

    public Map<String, Object> stats() {
        return Map.of(
                "httpCallsReceived", httpCallsReceived.get(),
                "chargesExecuted", chargesExecuted.get(),
                "storedKeys", store.size()
        );
    }

    public void reset() {
        store.clear();
        chargesExecuted.set(0);
        httpCallsReceived.set(0);
        log.info("Idempotent payment store reset");
    }
}
