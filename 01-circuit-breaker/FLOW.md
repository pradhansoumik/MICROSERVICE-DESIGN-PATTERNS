# Circuit Breaker — FLOW

Demo: Order Service (`:8082`) → Payment Service (`:8081`)

---

## Happy path (CLOSED)

```text
Client
  │  POST /api/orders
  ▼
OrderController
  │  orderService.placeOrder(request)
  ▼
OrderService
  │  paymentClient.pay(...)
  ▼
PaymentClient   ← @CircuitBreaker + @TimeLimiter
  │  RestClient → Payment Service
  ▼
PaymentController (/api/payments)
  │  SUCCESS
  ▼
OrderService → OrderResponse CONFIRMED
```

---

## Failure path → OPEN → fallback

```text
Client → OrderController → OrderService → PaymentClient
                                              │
                                    Payment FAIL / SLOW (timeout)
                                              │
                         CircuitBreaker records failure
                                              │
                    failureRate ≥ threshold → state OPEN
                                              │
                         next calls: NO call to Payment
                                              ▼
                                    payFallback() → PENDING
                                              ▼
                         OrderResponse PENDING_PAYMENT (usedFallback=true)
```

---

## Recovery

```text
OPEN ──(waitDuration)──► HALF_OPEN
                            │
                   probe calls to Payment
                     ├── success → CLOSED
                     └── fail    → OPEN
```

---

## Who calls whom

| Layer | Class | Role |
|---|---|---|
| API | `OrderController` | HTTP entry |
| Business | `OrderService` | Build order + interpret payment result |
| Resilience | `PaymentClient` | CB + TimeLimiter + fallback |
| Downstream | `PaymentController` | Can FAIL / SLOW / NONE |

**Note:** Circuit Breaker sits on the **caller** (`PaymentClient`), not on Payment Service itself.

---

## Interview line

> “Order API calls Payment through a Resilience4j Circuit Breaker. On repeated failures the breaker opens and we fail fast to a PENDING_PAYMENT fallback instead of cascading timeouts. In production we monitor breaker state via Actuator/Micrometer and tune thresholds per dependency.”

Related notes: `CB_INTERVIEW-REVISION-NOTES.md`, `DEMO.md`
