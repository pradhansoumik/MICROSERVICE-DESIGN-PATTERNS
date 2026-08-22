package com.interview.gateway.secure;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple FIXED WINDOW counter (in-memory) — demo only.
 * Production: Redis shared store + token bucket / sliding window.
 */
@Component
public class InMemoryRateLimiter {

    private final AppProperties props;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(AppProperties props) {
        this.props = props;
    }

    public Decision check(String key) {
        long windowMs = props.getRateLimit().getWindowSeconds() * 1000L;
        int limit = props.getRateLimit().getLimit();
        long now = Instant.now().toEpochMilli();
        long windowStart = (now / windowMs) * windowMs;

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || existing.windowStart != windowStart) {
                return new WindowCounter(windowStart, new AtomicInteger(0));
            }
            return existing;
        });

        int used = counter.count.incrementAndGet();
        boolean allowed = used <= limit;
        long resetSeconds = Math.max(1, (windowStart + windowMs - now) / 1000);

        return new Decision(allowed, limit, Math.max(0, limit - used), resetSeconds, used);
    }

    public record Decision(boolean allowed, int limit, int remaining, long retryAfterSeconds, int used) {
    }

    private static final class WindowCounter {
        private final long windowStart;
        private final AtomicInteger count;

        private WindowCounter(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
