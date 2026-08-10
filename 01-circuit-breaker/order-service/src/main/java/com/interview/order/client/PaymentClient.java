package com.interview.order.client;

import com.interview.order.dto.PaymentRequest;
import com.interview.order.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;

/**
 * Remote call to Payment Service — THIS is where Circuit Breaker sits.
 *
 * Interview talking point:
 * "I wrap the outbound dependency call (not the whole business method)
 *  so only payment failures trip the breaker. Order creation can still
 *  return a graceful fallback (PENDING_PAYMENT) instead of 500s."
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private static final String CB_NAME = "paymentService";

    private final RestClient restClient;

    public PaymentClient(@Value("${payment.service.url}") String paymentBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentBaseUrl)
                .build();
    }

    /**
     * Returns CompletableFuture because @TimeLimiter works with async types.
     * CircuitBreaker + TimeLimiter are composed via Resilience4j annotations.
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "payFallback")
    @TimeLimiter(name = CB_NAME)
    public CompletableFuture<PaymentResponse> pay(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Calling payment-service for orderId={}", request.orderId());

            return restClient.post()
                    .uri("/api/payments")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new PaymentServiceException(
                                "Payment service returned HTTP " + res.getStatusCode().value());
                    })
                    .body(PaymentResponse.class);
        });
    }

    /**
     * Fallback must match method signature + Throwable as last arg.
     * Real systems: enqueue for retry, mark order PENDING_PAYMENT, notify ops.
     */
    @SuppressWarnings("unused")
    private CompletableFuture<PaymentResponse> payFallback(PaymentRequest request, Throwable ex) {
        log.warn("FALLBACK triggered for orderId={} | reason={}",
                request.orderId(), ex.toString());

        return CompletableFuture.completedFuture(new PaymentResponse(
                null,
                request.orderId(),
                "PENDING",
                "Payment deferred — circuit open or payment unavailable: " + ex.getClass().getSimpleName()
        ));
    }

    public static class PaymentServiceException extends RuntimeException
    {
        public PaymentServiceException(String message)
        {
            super(message);
        }
    }
}
