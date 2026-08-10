package com.interview.retry;

/**
 * Business failure — should NOT be retried (e.g. insufficient funds).
 */
public class BusinessApiException extends RuntimeException {

    public BusinessApiException(String message) {
        super(message);
    }
}
