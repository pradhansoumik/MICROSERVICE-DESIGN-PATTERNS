# Retry Pattern — Interview Notes (quick)

Works with your Circuit Breaker demo: Order → Payment.

---

## 1. What problem does Retry solve?

Some failures are **transient** (temporary):
- momentary network glitch
- payment gateway returns 503 once
- DB/connection pool blip

If you retry **soon**, the next call often succeeds — without failing the whole order.

---

## 2. Retry vs Circuit Breaker vs Timeout

| Pattern | When | Behavior |
|---|---|---|
| **Timeout** | Every call | Don't wait forever (e.g. 2s) |
| **Retry** | Transient failure | Try again a few times |
| **Circuit Breaker** | Repeated failures | Stop calling; fail fast + fallback |

```text
Call Payment
   → Timeout cuts long waits
   → on transient error: Retry (2–3 times with backoff)
   → if still failing a lot: Circuit Breaker OPEN → fallback
```

**One-liner:** Retry helps **blips**; Circuit Breaker stops **outages**.

---

## 3. Real-life example (Order → Payment)

### Example A — Retry helps (transient)
```text
Attempt 1: Payment 503 (gateway overload)  → retry
wait 500ms
Attempt 2: Payment SUCCESS                → order CONFIRMED
```

### Example B — Retry should NOT help (business error)
```text
Attempt 1: Insufficient funds / invalid card
→ DO NOT retry (same result every time)
→ fail order with clear business error
```

### Example C — Retry + Circuit Breaker together
```text
CLOSED: retries allowed for transient errors
OPEN:   no call, no retry → fallback PENDING_PAYMENT immediately
```

---

## 4. Golden rules (interview must-say)

1. Retry only **idempotent** operations (or use **idempotency key**)
   - Safe-ish: GET, or PUT with same key, payment with `Idempotency-Key`
   - Dangerous: POST that charges twice without idempotency
2. Cap attempts (`maxAttempts=3`) — never infinite
3. Use **backoff** (+ jitter) — don’t hammer the dependency
4. Retry only **retryable** errors (408, 429, 502, 503, 504, timeouts)
5. Do **not** retry 400-level business failures (except 408/429)
6. When breaker is **OPEN**, skip retries

---

## 5. Resilience4j configuration (typical)

```properties
resilience4j.retry.instances.paymentService.maxAttempts=3
resilience4j.retry.instances.paymentService.waitDuration=500ms
resilience4j.retry.instances.paymentService.enableExponentialBackoff=true
resilience4j.retry.instances.paymentService.exponentialBackoffMultiplier=2
resilience4j.retry.instances.paymentService.retryExceptions[0]=java.io.IOException
resilience4j.retry.instances.paymentService.retryExceptions[1]=org.springframework.web.client.HttpServerErrorException
resilience4j.retry.instances.paymentService.ignoreExceptions[0]=com.interview.order.client.PaymentClient$BusinessPaymentException
```

### Meaning
| Property | Example | Meaning |
|---|---|---|
| `maxAttempts` | 3 | 1 original + 2 retries (check docs: includes first call) |
| `waitDuration` | 500ms | Base wait before next try |
| `enableExponentialBackoff` | true | 500ms → 1s → 2s … |
| `retryExceptions` | 5xx, IO | Only these trigger retry |
| `ignoreExceptions` | business errors | Never retry these |

### Annotation style
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "payFallback")
@Retry(name = "paymentService")
@TimeLimiter(name = "paymentService")
public CompletableFuture<PaymentResponse> pay(PaymentRequest request) { ... }
```

**Aspect order tip:** typically Retry around the call while Closed; CB still short-circuits when Open.

---

## 6. Backoff example (what to draw on whiteboard)

```text
maxAttempts = 3, waitDuration = 500ms, multiplier = 2

Try 1  fail
wait 500ms
Try 2  fail
wait 1000ms
Try 3  fail → give up → CB counts failure / fallback
```

**Jitter:** add random delay so many instances don’t retry in sync (thundering herd).

---

## 7. Idempotency example (payment)

Without idempotency:
```text
Try1: charge success, but response lost
Try2: charges AGAIN  → double payment 💥
```

With idempotency key:
```text
Idempotency-Key: ORD-123
Try1: charge once, store result
Try2: same key → return same result, no second charge ✅
```

---

## 8. 30-second pitch

> "I use Retry for transient failures with limited attempts and exponential backoff, and only for retryable errors. For payments I require an idempotency key so retries aren’t unsafe. If failures continue, Circuit Breaker opens and we stop retrying and use fallback. Retry handles blips; Circuit Breaker handles prolonged outages."

---

## 9. Mini Q&A

**Q: Should every API be retried?**  
No — only transient/retryable failures; never blind retry on business errors.

**Q: Retry at gateway or service?**  
Often at caller/client; gateway retries need care (duplication risk).

**Q: Retry + CB order?**  
Retry while CLOSED; when OPEN, fail fast (no retries).
