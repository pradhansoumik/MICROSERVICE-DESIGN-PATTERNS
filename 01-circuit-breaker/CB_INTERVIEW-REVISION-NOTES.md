# Circuit Breaker — Interview Revision Notes

**Demo:** Order Service (`:8082`) → Payment Service (`:8081`)  
**Library:** Resilience4j (`@CircuitBreaker` + `@TimeLimiter` + fallback)

---

## 1. Why Circuit Breaker?

If Payment is down/slow, Order threads wait/fail → under load this causes **cascading failure**.  
Circuit Breaker **fails fast**, stops calling the unhealthy dependency for a while, and returns a **fallback** (`PENDING_PAYMENT`).

---

## 2. States (must memorize)

```text
CLOSED ──(failure/slow rate high)──► OPEN ──(waitDuration)──► HALF_OPEN
   ▲                                                            │
   └────────────(probes OK)─────────────────────────────────────┤
                                                 (probes fail) ──► OPEN
```

| State | Behavior |
|---|---|
| **CLOSED** | Normal — calls go to Payment |
| **OPEN** | Fail fast — **no** Payment call → fallback immediately |
| **HALF_OPEN** | Allow limited **probe** calls to test recovery |

### Important rules
- **No direct OPEN → CLOSED** (must go through HALF_OPEN)
- User can **always place orders** in all 3 states (API stays up)
- Only the **payment call** is blocked/limited

---

## 3. Core Resilience4j configuration (order-service)

```properties
# ----- CLOSED → OPEN (sliding window) -----
resilience4j.circuitbreaker.instances.paymentService.slidingWindowSize=6
resilience4j.circuitbreaker.instances.paymentService.minimumNumberOfCalls=4
resilience4j.circuitbreaker.instances.paymentService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.paymentService.slidingWindowType=COUNT_BASED

# ----- OPEN → HALF_OPEN -----
resilience4j.circuitbreaker.instances.paymentService.waitDurationInOpenState=30s
resilience4j.circuitbreaker.instances.paymentService.automaticTransitionFromOpenToHalfOpenEnabled=true

# ----- HALF_OPEN probes -----
resilience4j.circuitbreaker.instances.paymentService.permittedNumberOfCallsInHalfOpenState=2

# ----- SLOW call detection (helps trip breaker on latency) -----
resilience4j.circuitbreaker.instances.paymentService.slowCallDurationThreshold=2s
resilience4j.circuitbreaker.instances.paymentService.slowCallRateThreshold=50

# ----- Hard timeout on payment call (returns fallback sooner) -----
resilience4j.timelimiter.instances.paymentService.timeoutDuration=2s
resilience4j.timelimiter.instances.paymentService.cancelRunningFuture=true
```

### Config meaning cheat-sheet

| Property | Value | Meaning |
|---|---|---|
| `slidingWindowSize` | 6 | Look at last N calls (CLOSED) |
| `minimumNumberOfCalls` | 4 | Don’t open until enough samples |
| `failureRateThreshold` | 50% | Open if ≥50% failed |
| `waitDurationInOpenState` | 30s | Stay OPEN before probing |
| `automaticTransitionFromOpenToHalfOpenEnabled` | true | OPEN→HALF_OPEN on timer (no call needed) |
| `permittedNumberOfCallsInHalfOpenState` | 2 | Max probe calls in HALF_OPEN |
| `slowCallDurationThreshold` | 2s | Call slower than this = slow |
| `slowCallRateThreshold` | 50% | Open if ≥50% calls are slow |
| `timelimiter.timeoutDuration` | 2s | Stop waiting; trigger fallback |

### Code wiring
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "payFallback")
@TimeLimiter(name = "paymentService")
public CompletableFuture<PaymentResponse> pay(...) { ... }
```

Fallback marks payment as `PENDING` → order status `PENDING_PAYMENT`.

---

## 4. Significance of `@TimeLimiter`

`@TimeLimiter` sets a **hard max wait time** for a call. If the dependency doesn’t finish in time, that attempt fails (timeout) instead of hanging forever.

### Supporting configuration

```properties
resilience4j.timelimiter.instances.paymentService.timeoutDuration=2s
resilience4j.timelimiter.instances.paymentService.cancelRunningFuture=true
```

- Instance name `paymentService` must match `@TimeLimiter(name = "paymentService")`
- Works with async return types (`CompletableFuture` / `CompletionStage`) — why `pay()` returns `CompletableFuture`

Related but **different** (CB slow-call metrics, not TimeLimiter itself):

```properties
resilience4j.circuitbreaker.instances.paymentService.slowCallDurationThreshold=2s
resilience4j.circuitbreaker.instances.paymentService.slowCallRateThreshold=50
```

### Is it only for slow response? What about direct failure?

**Its job is timeout / hung-or-slow calls.**

| Case | What fails the call | Need TimeLimiter? |
|---|---|---|
| **Direct failure** (503, connection error) | Exception comes back **quickly** | **Not required** for that failure to be detected |
| **Slow / hang** (no response for long) | Nothing fails until you stop waiting | **Yes** — otherwise threads wait forever |

- **FAIL demo:** Payment returns 503 fast → CB counts failure → fallback. TimeLimiter usually does **not** get involved.
- **SLOW demo:** Payment sleeps 3s → TimeLimiter cuts at **2s** → PENDING (even if Payment later returns SUCCESS).

### vs Circuit Breaker vs Retry

| Annotation | Job |
|---|---|
| `@TimeLimiter` | **This call** — don’t wait more than X |
| `@Retry` | Try again after a failure/timeout (limited times) |
| `@CircuitBreaker` | Across many calls — stop calling if too many failures/slow |

**One-liner:** `@TimeLimiter` protects against **slow** dependencies; `@CircuitBreaker` protects against **unhealthy** ones. For direct/fast failures CB is enough to see the error; in production you still keep **both** because failures can be fast *or* slow.

---

## 5. Which config applies on which transition?

| Transition | Automatic? | Config used |
|---|---|---|
| **CLOSED → OPEN** | No (needs failing/slow calls) | `slidingWindowSize`, `minimumNumberOfCalls`, `failureRateThreshold`, `slowCall*` |
| **OPEN → HALF_OPEN** | **Yes** (with your config) | `waitDurationInOpenState` + `automaticTransitionFromOpenToHalfOpenEnabled=true` |
| **HALF_OPEN → CLOSED** | **No** | Need successful **probe orders**; uses `permittedNumberOfCallsInHalfOpenState` + `failureRateThreshold` on probe results |
| **HALF_OPEN → OPEN** | No (needs failed probes) | Same half-open configs; if probe failure rate ≥ threshold → OPEN |
| **OPEN → CLOSED directly** | **Impossible** | Always OPEN → HALF_OPEN → CLOSED |

### HALF_OPEN decision (your values)
- Allow **2** probes
- After 2 finish:
  - failure rate **< 50%** → **CLOSED**
  - failure rate **≥ 50%** → **OPEN** again  
- So **1 fail + 1 success (50%)** → OPEN; **2 success** → CLOSED

---

## 6. Scenario A — Happy path (`NONE`)

**Payment:** responds quickly with SUCCESS  

**Flow:**
```text
Place order → Payment OK → order CONFIRMED → breaker stays CLOSED
```

**Config involved:** none special; breaker remains CLOSED.

---

## 7. Scenario B — FAIL (hard dependency error)

**Payment config/mode:**
```properties
# runtime: POST /api/payments/failure-mode?mode=FAIL
```
Payment returns HTTP 503 / error.

**Flow:**
```text
CLOSED
  → several failed payment calls (need ≥ minimumNumberOfCalls=4)
  → failureRate ≥ 50%
  → OPEN  (fallback: PENDING_PAYMENT, no further payment calls)
  → wait 30s
  → HALF_OPEN (automatic)
  → place orders as probes
      → if Payment still FAIL → OPEN again
      → if Payment healed (NONE) + probes OK → CLOSED
```

**Configs that matter here:**
- `minimumNumberOfCalls=4`
- `failureRateThreshold=50`
- `slidingWindowSize=6`
- `waitDurationInOpenState=30s`
- `automaticTransitionFromOpenToHalfOpenEnabled=true`
- `permittedNumberOfCallsInHalfOpenState=2`

**Demo tip:** Check `/api/orders/circuit-status` **immediately** after failing orders to catch `OPEN`. If you wait >30s you will already see `HALF_OPEN` (normal).

---

## 8. Scenario C — SLOW (latency / timeout)

**Payment:**
```properties
payment.slow.delay-ms=3000   # sleeps 3s then returns SUCCESS
```
Mode: `SLOW`

**Order:**
```properties
resilience4j.timelimiter.instances.paymentService.timeoutDuration=2s
resilience4j.circuitbreaker.instances.paymentService.slowCallDurationThreshold=2s
resilience4j.circuitbreaker.instances.paymentService.slowCallRateThreshold=50
```

**Why response is PENDING even though Payment code returns SUCCESS:**
```text
0s ---------- 2s -------------------- 3s
              │                       │
         TimeLimiter timeout     Payment returns SUCCESS
         → fallback PENDING      (too late; caller already left)
```

- **Immediate PENDING** comes from **TimeLimiter `timeoutDuration=2s`** + fallback  
- **Breaker OPEN** later comes from repeated timeouts / **slow-call rate ≥ 50%**

**FAIL vs SLOW:**

| Mode | Payment behavior | Why user sees PENDING / breaker opens |
|---|---|---|
| FAIL | Error/503 | Failure rate |
| SLOW | Success after 3s | Timeout (2s) + slow-call threshold |

**Interview line:** Fallback is based on **caller timeout**, not whether downstream would succeed later.

---

## 9. Can user place order in each state?

| State | Place order? | Payment called? |
|---|---|---|
| CLOSED | Yes | Yes (always) |
| OPEN | Yes | **No** → fallback |
| HALF_OPEN | Yes | Only up to **2 probes**; extras → fallback |

---

## 10. Metrics note (`/api/orders/circuit-status`)

`numberOfSuccessfulCalls` / `numberOfFailedCalls` are **current window** metrics, **not lifetime counters**.

- After **HALF_OPEN → CLOSED**, success count may reset to **0** (fresh window) — normal  
- On **HALF_OPEN → OPEN**, failed calls increase — expected  
- Trust **`state`** more than raw counters for the story

---

## 11. What cannot be configured as “automatic”

| Wish | Possible? |
|---|---|
| Auto OPEN → HALF_OPEN | **Yes** (`automaticTransition...=true`) |
| Auto HALF_OPEN → CLOSED (timer only) | **No** — needs successful probes |
| Auto OPEN → CLOSED directly | **No** |

---

## 12. 90-second interview pitch

> "Payment is a critical dependency for orders. I wrap the payment client with Resilience4j CircuitBreaker and TimeLimiter.  
> If failure or slow-call rate crosses 50% in a sliding window (after minimum calls), the breaker opens and we fail fast to a fallback that marks the order PENDING_PAYMENT — protecting our threads from cascading timeouts.  
> After waitDuration, we enter HALF_OPEN and allow a few probe calls. Probe success closes the circuit; probe failure opens it again.  
> We never jump OPEN→CLOSED, and HALF_OPEN→CLOSED is never timer-only — recovery must be proven."

---

## 13. High-probability Q&A

**Q: CB vs Retry?**  
Retry = transient blips. CB = stop calling when clearly unhealthy. Often combined (limited retries while CLOSED).

**Q: CB vs TimeLimiter?**  
TimeLimiter = per-call max wait (needed mainly for slow/hang). CB = state machine across many calls. Direct/fast failures don’t need TimeLimiter to be detected; still keep both in production.

**Q: Is TimeLimiter only for SLOW, not FAIL?**  
Yes for its *purpose* — timeout. FAIL returns error quickly without it. SLOW/hang needs `timeoutDuration` or threads can block.

**Q: Where to put breaker?**  
On the **caller**, around the specific outbound dependency.

**Q: Fallback best practice?**  
Degrade safely (`PENDING_PAYMENT`). Never pretend payment succeeded if money wasn’t captured.

**Q: Hystrix vs Resilience4j?**  
Hystrix maintenance mode; Resilience4j is the current standard with Spring Boot.

---

## 14. Quick demo checklist

1. Start payment `:8081`, order `:8082`
2. `NONE` → order → `CONFIRMED`, state `CLOSED`
3. `FAIL` → 5–6 orders → check status ASAP → `OPEN`
4. Wait 30s → `HALF_OPEN` (automatic)
5. Keep FAIL + probe orders → back to `OPEN`
6. Set `NONE` + probe orders → `CLOSED`
7. `SLOW` → PENDING via 2s timeout even though payment succeeds at 3s

**Useful URLs**
- `POST /api/orders`
- `GET /api/orders/circuit-status`
- `POST /api/payments/failure-mode?mode=FAIL|SLOW|NONE`
