package com.interview.retry;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo: Retry first (transient attempts), then Circuit Breaker opens on sustained failure.
 *
 * Flow per HTTP request while CLOSED:
 *   attempt1 fail → retry → attempt2 fail → retry → attempt3 fail
 *   → fallback (RETRY_EXHAUSTED) and CB records failures
 *
 * After failure rate threshold → OPEN:
 *   next HTTP call → CallNotPermitted → fallback (CIRCUIT_OPEN) without calling AlwaysFailApi
 */
@Service
public class RetryThenCircuitBreakerService {

    private static final Logger log = LoggerFactory.getLogger(RetryThenCircuitBreakerService.class);
    private static final String NAME = "retryThenCb";

    private final AlwaysFailApi alwaysFailApi;

    public RetryThenCircuitBreakerService(AlwaysFailApi alwaysFailApi) {
        this.alwaysFailApi = alwaysFailApi;
    }

    @CircuitBreaker(name = NAME, fallbackMethod = "fallback")
    @Retry(name = NAME, fallbackMethod = "fallback")
    public Map<String, Object> callWithRetryThenCb() {
        log.info("HTTP business call entered (Retry + CB enabled)");
        alwaysFailApi.call(); // always throws
        // unreachable
        return Map.of("outcome", "SUCCESS");
    }

    @SuppressWarnings("unused")
    private Map<String, Object> fallback(Throwable ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("usedFallback", true);
        body.put("exception", ex.getClass().getSimpleName());
        body.put("message", ex.getMessage());

        if (ex instanceof CallNotPermittedException) {
            log.warn("CIRCUIT OPEN → fail-fast fallback (no downstream calls / no retries to dependency)");
            body.put("outcome", "CIRCUIT_OPEN_FALLBACK");
            body.put("phase", "CircuitBreaker OPEN — Retry does not hammer dependency");
        } else {
            log.warn("Retries exhausted → fallback. reason={}", ex.toString());
            body.put("outcome", "RETRY_EXHAUSTED_FALLBACK");
            body.put("phase", "Retry tried maxAttempts — failure recorded toward Circuit Breaker");
        }
        return body;
    }
}
