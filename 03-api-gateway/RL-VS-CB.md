# Rate Limiting vs Circuit Breaker

Quick comparison for interview revision.  
**(Common trap:** “Why do we need Circuit Breaker if Rate Limiting already controls execution?”)

---

## Side-by-side

| | **Rate Limiting (RL)** | **Circuit Breaker (CB)** |
|---|---|---|
| **Problem** | Too many requests from clients (abuse / fairness / quota) | Dependency is unhealthy (errors / timeouts / slow) |
| **Direction** | **Inbound** — clients → your API | **Outbound** — your service → Payment / other dependency |
| **Question** | “Is this user / IP over quota?” | “Is Payment broken right now?” |
| **Trigger** | Count / tokens in a time window | Failure rate % / slow-call rate |
| **Action** | Reject that client → **429** | **OPEN** → fail fast / fallback; **do not call** dependency |
| **Key** | userId / IP / API key (per client) | Downstream service name (shared across all users) |
| **Typical place** | API Gateway (edge) | Inside the caller service (Order → Payment) |
| **Risk if missing** | One client floods you; unfair usage | Cascading failure / thread pool exhaustion waiting on Payment |

```text
Client → [Rate Limit at Gateway] → Order Service → [Circuit Breaker] → Payment
              ↑ inbound                              ↑ outbound
```

**One-liner:** Rate limiting is **client fairness**. Circuit breaker is **dependency isolation**. Neither replaces the other.

---

## Why RL alone is not enough

1. **Many users, each under limit**  
   Alice 5/min + Bob 5/min × 1000 users → Payment still flooded.  
   RL is per client; CB looks at **Payment’s health**.

2. **RL does not know the dependency is dying**  
   All calls can be “allowed” by RL while Payment returns 500 / times out.

3. **Fail-fast vs timeout storm**  
   Without CB: threads wait on Payment.  
   With CB **OPEN**: quick fallback; system stays responsive.

---

## Does OPEN mean no call to downstream?

**Yes — while OPEN, the dependency is not called.**

| CB state | Calls Payment? |
|---|---|
| **CLOSED** | Yes — normal |
| **OPEN** | **No** — fail fast / fallback |
| **HALF_OPEN** | Only a few **probe** calls |

```text
CLOSED:     Order → Payment → response
OPEN:       Order → (block) → fallback     ← Payment not hit
HALF_OPEN:  Order → Payment (trial) → decide CLOSED or OPEN again
```

CB is not “never call forever” — it **stops calling while sick**, then carefully probes later.

---

## Real-life analogy

### Rate Limiting → Ticket counter / queue limit

A cinema counter: **only 5 tickets per person per hour**.  
If you ask for a 6th → “come back later” (**429**).  
Does **not** tell you whether the projector inside is broken.

### Circuit Breaker → Out-of-order lift (same as Retry-vs-CB)

Lift keeps failing → **OUT OF ORDER** board (**OPEN**) → take stairs (**fallback**) → later allow 1–2 test rides (**HALF_OPEN**).

### Together → Online checkout

| What happens | Pattern |
|---|---|
| User spams Place Order 100 times/sec → gateway returns 429 | **Rate Limiting** |
| Payment bank is down → Order stops calling Payment, marks PENDING | **Circuit Breaker** |
| 1000 polite users (each under RL) still hit a dying Payment | Need **CB** — RL alone won’t save you |

### Interview analogy line

> “Rate limiting is the ticket counter — how many requests each customer may make. Circuit breaker is the out-of-order lift — stop using a broken dependency and take the stairs until it’s safe to try again.”

---

## Interview pitch (30 seconds)

> “Rate limiting controls inbound client volume — fairness and abuse. Circuit breaker protects outbound calls to a dependency — when Payment is unhealthy we open the circuit and stop calling it. Many users each under their rate limit can still collectively kill Payment, and RL doesn’t watch failure rates. In production I use both: RL at the gateway, CB on service-to-service calls.”

---

## Related notes / demos

| Topic | Path |
|---|---|
| Design a rate limiter | `DESIGN-RATE-LIMITING-SYSTEM.md` |
| RL demo (gateway) | `api-gateway-auth-ratelimit` (:8085) |
| CB interview notes | `../01-circuit-breaker/CB_INTERVIEW-REVISION-NOTES.md` |
| Retry vs CB | `../01-circuit-breaker/RETRY-VS-CB.md` |
| CB demo | `../01-circuit-breaker/order-service` (:8082) → `payment-service` (:8081) |
