package com.interview.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dummy dependency that ALWAYS fails (simulates prolonged outage).
 * Used to show: Retry tries a few times → then Circuit Breaker opens.
 */
@Component
public class AlwaysFailApi {

    private static final Logger log = LoggerFactory.getLogger(AlwaysFailApi.class);
    private final AtomicInteger attempts = new AtomicInteger(0);

    public void call() {
        int n = attempts.incrementAndGet();
        log.info("AlwaysFailApi attempt #{} → throwing TransientApiException", n);
        throw new TransientApiException("Downstream permanently unavailable (attempt " + n + ")");
    }

    public void reset() {
        attempts.set(0);
        log.info("AlwaysFailApi counter reset");
    }

    public int currentAttempts() {
        return attempts.get();
    }
}
