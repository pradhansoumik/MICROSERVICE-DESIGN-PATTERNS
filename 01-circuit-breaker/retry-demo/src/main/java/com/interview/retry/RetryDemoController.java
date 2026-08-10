package com.interview.retry;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RetryDemoController {

    private final RetryCallerService retryCallerService;
    private final RetryThenCircuitBreakerService retryThenCircuitBreakerService;
    private final FlakyApi flakyApi;
    private final AlwaysFailApi alwaysFailApi;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public RetryDemoController(
            RetryCallerService retryCallerService,
            RetryThenCircuitBreakerService retryThenCircuitBreakerService,
            FlakyApi flakyApi,
            AlwaysFailApi alwaysFailApi,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.retryCallerService = retryCallerService;
        this.retryThenCircuitBreakerService = retryThenCircuitBreakerService;
        this.flakyApi = flakyApi;
        this.alwaysFailApi = alwaysFailApi;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Main demo: flaky API fails first 2 times, Retry retries, 3rd attempt succeeds.
     */
    @GetMapping("/demo/retry")
    public Map<String, Object> demoRetry() {
        return retryCallerService.callWithRetry();
    }

    /**
     * Shows business error is not retried (only 1 attempt in logs).
     */
    @GetMapping("/demo/business")
    public Map<String, Object> demoBusiness() {
        return retryCallerService.callBusinessWithRetry();
    }

    /**
     * Retry + Circuit Breaker:
     * Call several times → first calls show RETRY_EXHAUSTED (3 attempts in logs),
     * then state becomes OPEN → CIRCUIT_OPEN_FALLBACK (no AlwaysFailApi logs).
     */
    @GetMapping("/demo/retry-then-cb")
    public Map<String, Object> demoRetryThenCb() {
        Map<String, Object> result = new LinkedHashMap<>(retryThenCircuitBreakerService.callWithRetryThenCb());
        result.put("circuit", circuitSnapshot());
        return result;
    }

    @GetMapping("/demo/retry-then-cb/status")
    public Map<String, Object> retryThenCbStatus() {
        return circuitSnapshot();
    }

    @PostMapping("/demo/retry-then-cb/reset")
    public Map<String, Object> resetRetryThenCb() {
        alwaysFailApi.reset();
        circuitBreakerRegistry.circuitBreaker("retryThenCb").reset();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reset", true);
        body.put("alwaysFailAttempts", alwaysFailApi.currentAttempts());
        body.put("circuit", circuitSnapshot());
        return body;
    }

    /** Direct call WITHOUT retry — useful to compare. */
    @GetMapping("/flaky")
    public Map<String, Object> flakyDirect() {
        return flakyApi.call();
    }

    @PostMapping("/flaky/reset")
    public Map<String, Object> reset() {
        flakyApi.reset();
        return Map.of("reset", true, "attempts", flakyApi.currentAttempts());
    }

    private Map<String, Object> circuitSnapshot() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("retryThenCb");
        CircuitBreaker.Metrics m = cb.getMetrics();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("name", cb.getName());
        status.put("state", cb.getState().name());
        status.put("failureRate", m.getFailureRate());
        status.put("numberOfFailedCalls", m.getNumberOfFailedCalls());
        status.put("numberOfSuccessfulCalls", m.getNumberOfSuccessfulCalls());
        status.put("numberOfNotPermittedCalls", m.getNumberOfNotPermittedCalls());
        return status;
    }
}
