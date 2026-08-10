package com.interview.retry;

/**
 * Simulates a transient / retryable failure (like HTTP 503 from payment gateway).
 */
public class TransientApiException extends RuntimeException {

    public TransientApiException(String message) {
        super(message);
    }
}
