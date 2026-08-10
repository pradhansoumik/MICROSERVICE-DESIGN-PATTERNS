package com.interview.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dummy "remote" API behavior living in-process for a simple demo.
 * First N calls fail with TransientApiException, then succeed.
 */
@Component
public class FlakyApi {

    private static final Logger log = LoggerFactory.getLogger(FlakyApi.class);

    private final AtomicInteger attempts = new AtomicInteger(0);

    @Value("${demo.flaky.fail-times:2}")
    private int failTimes;

    public Map<String, Object> call() {
        int n = attempts.incrementAndGet();
        log.info("FlakyApi attempt #{} (fails first {} times)", n, failTimes);

        if (n <= failTimes) {
            throw new TransientApiException("Dummy API temporarily unavailable on attempt " + n);
        }

        return Map.of(
                "status", "SUCCESS",
                "attempt", n,
                "message", "Dummy API succeeded on attempt " + n
        );
    }

    /** Always fails with business error — Retry must NOT retry this. */
    public Map<String, Object> callBusinessFailure() {
        throw new BusinessApiException("Insufficient funds — do not retry");
    }

    public void reset() {
        attempts.set(0);
        log.info("FlakyApi counter reset");
    }

    public int currentAttempts() {
        return attempts.get();
    }
}
