# Circuit Breaker — Internal Working (Easy Recall)

**Goal:** Answer *“How does CB work internally?”* and *“How does `@CircuitBreaker` work?”*  
**Demo:** Order → Payment (`PaymentClient` + Resilience4j)

> Yes — this version keeps the **simple A→H structure**, and restores important points that were in the longer note (time window, Hystrix, in-memory state, metrics reset, annotation traps, longer pitch).

---

## A. Remember in 4 layers

```text
1. STATE MACHINE     → CLOSED / OPEN / HALF_OPEN
2. METRICS WINDOW    → success / fail / slow (sliding window)
3. DECISION          → allow call OR fail-fast (+ fallback)
4. SPRING ANNOTATION → AOP proxy + CircuitBreakerAspect
```

**What sits around the call**
```text
Caller → [ Circuit Breaker: check state → allow/block → record → maybe transition ] → Payment
                              ↓ on fail / OPEN
                           fallback
```

---

## B. State machine (draw this)

```text
CLOSED ──(fail/slow rate high)──► OPEN ──(waitDuration)──► HALF_OPEN
  ▲                                                         │
  └──────────── probe OK ───────────────────────────────────┤
                                              probe fail ───► OPEN
```

| State | Call Payment? | Internal behavior |
|---|---|---|
| CLOSED | Yes | Record every result in window |
| OPEN | **No** | `CallNotPermittedException` / fallback; count `notPermitted` |
| HALF_OPEN | Limited probes only | After probes: OK → CLOSED; fail → OPEN |

**Rules**
- No direct **OPEN → CLOSED**
- **HALF_OPEN → CLOSED** = successful probes (not timer alone)
- **OPEN → HALF_OPEN** = after `waitDuration` (can be automatic if `automaticTransitionFromOpenToHalfOpenEnabled=true`)

---

## C. Metrics window (how it decides)

**Idea:** not 1 failure → open; look at **recent calls**.

### Knobs (our demo)

| Knob | Value | Meaning |
|---|---|---|
| `slidingWindowType` | COUNT_BASED | Last N **calls** (alt: TIME_BASED = last N **seconds**) |
| `slidingWindowSize` | 6 | Window size |
| `minimumNumberOfCalls` | 4 | Don’t open until enough samples |
| `failureRateThreshold` | 50% | Open if fails ≥ 50% |
| `slowCallDurationThreshold` | 2s | Slower than this = slow |
| `slowCallRateThreshold` | 50% | Open if too many slow |
| `waitDurationInOpenState` | 30s | Stay OPEN |
| `permittedNumberOfCallsInHalfOpenState` | 2 | Probe budget |
| `automaticTransitionFromOpenToHalfOpenEnabled` | true | Auto OPEN→HALF_OPEN on timer |

### Counts as failure / open signal
- Exception (e.g. HTTP 503 → exception)
- Timeout (`@TimeLimiter`)
- Slow call (completed but too slow)

### HALF_OPEN decision (detail)
- Allow up to **2** probes  
- After those finish: failure rate **&lt; 50%** → CLOSED; **≥ 50%** → OPEN again  
- Example: 1 fail + 1 success = 50% → OPEN again (with threshold 50)

### Metrics trap
- Success/fail counters are mostly **window metrics**, not lifetime  
- Often **reset** when entering CLOSED after recovery  
- Trust **`state`** more than raw numbers  

---

## D. One request — internal steps

### CLOSED
```text
1. Permit call
2. Execute Payment (optional TimeLimiter)
3. Record SUCCESS / FAILURE / SLOW
4. If samples ≥ minimum AND rate ≥ threshold → OPEN
5. Return result OR fallback
```

### OPEN
```text
1. Do NOT call Payment
2. Fail-fast → fallback / CallNotPermittedException
3. Increment notPermittedCalls
4. After waitDuration → HALF_OPEN (auto or on next attempt)
```

### HALF_OPEN
```text
1. Allow only permitted probe calls
2. Extra calls may be not-permitted until decision
3. Probes OK → CLOSED | Probes fail → OPEN
```

---

## E. `@CircuitBreaker` annotation (Spring internals + interview angle)

### E1. One line (say this first)
> “`@CircuitBreaker` does not change the method body. Spring creates a **proxy**; Resilience4j’s **CircuitBreakerAspect** intercepts the call, applies the state machine for the named breaker, records the result, and can invoke `fallbackMethod`.”

### E2. Call path (draw in interview)
```text
OrderService → paymentClient.pay()
        ↓
   Spring PROXY (bean)
        ↓
 CircuitBreakerAspect
   1. CircuitBreakerRegistry → breaker named "paymentService"
   2. OPEN? → skip real method → fallback
   3. else → proceed to real method
   4. record SUCCESS / FAILURE / SLOW → maybe OPEN/CLOSE
        ↓
 PaymentClient.pay() real body (HTTP)
        ↓ on error / open / timeout
 payFallback(request, Throwable)   ← same class, called by aspect
```

### E3. What you need for annotation to work
- Dependency: `resilience4j-spring-boot3`
- AOP: `spring-boot-starter-aop`
- Bean: class is `@Service` / `@Component` (Spring-managed)
- `name` in annotation = key in `application.properties`
- Optional: `fallbackMethod` in **same class**

### E4. Annotation traps (plain English)

**1) Self-call — `this.pay()`**
```java
public void placeOrder() {
    this.pay(...);   // BAD — call inside same class
}
```
CB runs only when another bean calls `paymentClient.pay()` through the **Spring proxy**.  
`this.pay()` skips the proxy → `@CircuitBreaker` does **not** run.

**2) `private` method**
```java
@CircuitBreaker(...)
private CompletableFuture<...> pay(...) { }  // usually NOT intercepted
```
Keep the annotated method **public** on a Spring bean.

**3) Wrong `name`**
```java
@CircuitBreaker(name = "paymentService")
```
must match:
```properties
resilience4j.circuitbreaker.instances.paymentService.failureRateThreshold=50
```
If names differ, your tuned config is not applied.

**4) Bad fallback signature**
```java
pay(PaymentRequest request)
// fallback must be:
payFallback(PaymentRequest request, Throwable ex)
```
Wrong signature → fallback ignored → exception bubbles to caller.

### E5. Interview Q&A (annotation-focused)

**Q1: Where should you put `@CircuitBreaker` — controller or client?**  
**A:** Prefer on the **outbound client / integration method** (e.g. `PaymentClient.pay`), not on the whole controller.  
Reason: breaker protects **one dependency**. Controller may do more than payment.

**Q2: Annotation vs programmatic API — which one?**  
**A:**  
- Annotation (`@CircuitBreaker`) → simple, declarative, common in Spring Boot  
- Programmatic (`CircuitBreakerRegistry` + `decorateSupplier` / `executeSupplier`) → more control, useful in libraries or dynamic names  

In interviews, say you used annotation for clarity; you know programmatic exists.

**Q3: What exactly does the aspect do on each call?**  
**A (structured):**  
1. Resolve breaker by `name` from registry  
2. Check state → allow or fail-fast  
3. If allowed, invoke real method  
4. Record success/failure/slow into sliding window  
5. Transition state if threshold crossed  
6. On failure/open, call `fallbackMethod` if configured  

**Q4: Does fallback run only when OPEN?**  
**A:** No. Fallback can run when:  
- dependency throws  
- timeout (`TimeLimiter`)  
- breaker OPEN (`CallNotPermittedException`)  

**Q5: Can one class have multiple breakers?**  
**A:** Yes — different methods can use different `name`s (`paymentService`, `inventoryService`). Each name = separate state/metrics.

**Q6: What if fallback itself fails?**  
**A:** Exception propagates to caller (unless handled higher). Keep fallback **simple and safe** (no remote call, or protected separately).

**Q7: `@CircuitBreaker` + `@Retry` + `@TimeLimiter` — what do you tell interviewer?**  
**A:** They compose via AOP around the same method:  
- **TimeLimiter** → max wait for this call  
- **Retry** → retry transient failures (while still useful)  
- **CircuitBreaker** → stop calling when unhealthy  

Concept stack (remember behavior, not exact order numbers):
```text
Retry → CircuitBreaker → TimeLimiter → real call
```
When OPEN, don’t keep hammering with retries.

**Q8: Is breaker shared across all pods/instances?**  
**A:** By default **no** — state is **in-memory per JVM**. Each instance has its own breaker. (Distributed CB is uncommon; usually each instance protects itself.)

**Q9: How do you verify it in production/interview demo?**  
**A:**  
- Actuator: `/actuator/circuitbreakers`, health  
- Custom status API (our `/api/orders/circuit-status`)  
- Metrics (Micrometer/Prometheus): state, failure rate, not-permitted calls  

**Q10: Why CompletableFuture with `@TimeLimiter`?**  
**A:** Resilience4j `@TimeLimiter` works with async types (`CompletableFuture` / `CompletionStage`). That’s why our `pay()` returns `CompletableFuture`.

### E6. Mini pitch (annotation only — 20 sec)
> “`@CircuitBreaker` is AOP-based. Spring proxies the bean; CircuitBreakerAspect uses the named breaker from config to allow or fail-fast, records outcomes, and can call a same-class fallbackMethod. I put it on the payment client method so only that dependency is protected.”

**Where is state stored?**
- Usually **in-memory per JVM instance**  
- 3 app instances = 3 separate breakers (unless a shared/distributed solution)

---

## F. Fallback vs state machine (plain English)

These are **two different jobs**:

**1) State machine (Circuit Breaker itself)**  
Decides: *“Should I call Payment right now?”*
- CLOSED → yes, call Payment  
- OPEN → no, don’t call Payment  
- HALF_OPEN → only a few test calls  

It also **records** success/fail/slow and may change state.

**2) Fallback (what you return to the user/API)**  
Decides: *“If Payment failed or breaker is OPEN, what safe answer do I give?”*

In our demo:
```text
Payment down / breaker OPEN
        ↓
payFallback(...)
        ↓
return PENDING (payment deferred)
        ↓
Order API shows PENDING_PAYMENT
```

Important: fallback must be **honest**.  
Don’t return “payment success” if money was not taken.

```text
State machine = traffic light for the dependency call
Fallback      = what you do when the light is red (or call failed)
```

---

### Circuit Breaker vs Retry (don’t confuse)

**Retry** = “try again a few times” for a **short glitch**  
Example: Payment returns 503 once → wait → try again → success.

**Circuit Breaker** = “stop calling for a while” when Payment is **clearly unhealthy**  
Example: many failures → OPEN → don’t call Payment; use fallback instead.

**How they work together**
```text
While CLOSED:  Retry may help with blips
When OPEN:     No point retrying Payment — fail-fast + fallback
```

**One line**  
> Retry fixes temporary blips; Circuit Breaker stops traffic during an outage; fallback is the safe response.

---

## G. Resilience4j vs Hystrix (say if asked)

| | Resilience4j | Hystrix |
|---|---|---|
| Status | Current standard with Spring Boot | Maintenance / legacy |
| Style | Functional, lightweight, Micrometer | Older Netflix library |
| Features | CB + Retry + TimeLimiter + Bulkhead + slow calls | Classic CB |

---

## H. Pitches (pick one)

### 30 seconds
> “CB is a state machine around a dependency call. It records results in a sliding window; high failure/slow rate opens the circuit and fail-fasts to fallback. After waitDuration, HALF_OPEN probes decide CLOSED or OPEN again. `@CircuitBreaker` works via Spring AOP: a proxy and CircuitBreakerAspect apply that logic using the named instance from config.”

### 60–90 seconds (full)
> “Circuit Breaker wraps the outbound call as CLOSED/OPEN/HALF_OPEN. In CLOSED it executes the call and records outcomes in a sliding window (count- or time-based). When failure or slow-call rate exceeds the threshold after minimum calls, it opens and fails fast—no dependency call—usually via fallback. After waitDuration it enters HALF_OPEN and allows a few probes; success closes, failure opens again. In Spring, `@CircuitBreaker` is AOP: the proxy intercepts the method, the registry loads the named breaker, and the aspect records results and can invoke `fallbackMethod`. State is typically in-memory per instance; we also use TimeLimiter so timeouts feed the same failure/slow signals.”

---

## I. Linked files

- `FLOW.md` — request path  
- `CB_INTERVIEW-REVISION-NOTES.md` — FAIL / SLOW / TimeLimiter demos  
- `RETRY-VS-CB.md` — Retry vs CB + real-life analogy  
- `DEMO.md` — how to run  
- `PaymentClient.java` + `application.properties`
