# Retry vs Circuit Breaker

Quick comparison for interview revision.

---

## Side-by-side

| | **Retry** | **Circuit Breaker (CB)** |
|---|---|---|
| **Problem** | Temporary / transient failure | Dependency is unhealthy for a while |
| **Action** | Try the same call again (limited times) | Stop calling; fail fast |
| **When** | Blip (busy once, one 503, short glitch) | Repeated failures / sustained outage |
| **Wait** | Short backoff between attempts | Stay OPEN for `waitDuration`, then probe |
| **Fallback** | Optional after retries exhausted | Usually yes while OPEN |
| **Risk if misused** | Double side-effects (need idempotency) | Stale/degraded response too long |
| **Together** | Used while CB is **CLOSED** | When **OPEN**, no retries |

```text
Call dependency
   → Timeout (don't wait forever)
   → on transient error: Retry (2–3 times + backoff)
   → if keep failing: Circuit Breaker OPEN → fallback
```

**One-liner:** Retry handles **blips**; Circuit Breaker handles **outages**.

---

## Real Life Analogy

### Retry → Calling a busy friend

You call a friend. Line is busy / no answer once.

- You wait a bit and **call again** (attempt 2)
- Still busy → wait longer → **call again** (attempt 3)
- Then they pick up → success

That’s **Retry**: temporary problem, try a few more times with waiting in between.

You would **not** keep calling 100 times nonstop — that’s rude (and like hammering a server).  
You also wouldn’t retry if they clearly said **“I won’t talk about this”** (business failure) — retrying won’t help.

### Circuit Breaker → Lift (elevator) out of order

The lift keeps failing / getting stuck.

1. **CLOSED (normal):** people keep using the lift  
2. After several failures → staff puts **“OUT OF ORDER”** board → **OPEN**  
   - Nobody tries the lift for a while (fail fast)  
   - People take the **stairs** instead → that’s **fallback**  
3. After some time, technician allows **1–2 test rides** → **HALF_OPEN**  
   - If tests work → remove board → **CLOSED** (normal again)  
   - If still broken → board back → **OPEN**

That’s **Circuit Breaker**: stop using something clearly broken; take stairs; probe later before full use.

### Together → UPI / shop payment

| What happens | Pattern |
|---|---|
| App says “network glitch, try again” → you tap pay once more → success | **Retry** |
| Payment server is down for 10 minutes → app stops hammering it, shows “Pay later / cash” | **Circuit Breaker + fallback** |
| You wait forever on spinning loader | Missing **Timeout** (bad UX) |

### Interview analogy line

> “Retry is like calling again when the line is busy. Circuit Breaker is like an out-of-order lift — stop using it, take the stairs, and test later before letting everyone back in.”

---

## Related demo folders

- Retry dummy API: `01-circuit-breaker/retry-demo`
- Circuit Breaker Order→Payment: `01-circuit-breaker/order-service` + `payment-service`
