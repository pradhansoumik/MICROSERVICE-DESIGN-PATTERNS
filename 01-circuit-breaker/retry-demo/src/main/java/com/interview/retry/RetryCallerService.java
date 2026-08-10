package com.interview.retry;

import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RetryCallerService {

    private static final Logger log = LoggerFactory.getLogger(RetryCallerService.class);

    private final FlakyApi flakyApi;

    public RetryCallerService(FlakyApi flakyApi) {
        this.flakyApi = flakyApi;
    }

    /**
     * Retries TransientApiException up to maxAttempts (see application.properties).
     */
    @Retry(name = "flakyApi", fallbackMethod = "transientFallback")
    public Map<String, Object> callWithRetry() {
        log.info("Calling flaky dummy API (Retry enabled)");
        Map<String, Object> result = flakyApi.call();
        return Map.of(
                "outcome", "SUCCESS_AFTER_RETRY_OR_FIRST_TRY",
                "usedFallback", false,
                "api", result
        );
    }

    /**
     * Business error is NOT in retryExceptions → no retry, goes straight to fallback (or throws).
     */
    @Retry(name = "flakyApi", fallbackMethod = "businessFallback")
    public Map<String, Object> callBusinessWithRetry() {
        return flakyApi.callBusinessFailure();
    }

    @SuppressWarnings("unused")
    private Map<String, Object> transientFallback(Throwable ex) {
        log.warn("Retry exhausted → fallback. reason={}", ex.toString());
        return Map.of(
                "outcome", "FAILED_AFTER_RETRIES",
                "usedFallback", true,
                "message", ex.getMessage()
        );
    }

    @SuppressWarnings("unused")
    private Map<String, Object> businessFallback(Throwable ex) {
        log.warn("Business failure (should be 1 attempt only). reason={}", ex.toString());
        return Map.of(
                "outcome", "BUSINESS_ERROR_NO_RETRY",
                "usedFallback", true,
                "message", ex.getMessage()
        );
    }
}
