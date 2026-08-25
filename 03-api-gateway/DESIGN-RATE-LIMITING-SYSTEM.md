# Design a Rate Limiting System (Interview)

**Why this note:** Interviewers often ask *“Design a rate limiting system”* as a system-design question.  
**Demo companion:** `api-gateway-auth-ratelimit` (:8085) — JWT auth + simple in-memory limiter at gateway.

---

## 1. Clarify requirements (always ask)

| Question | Example answer |
|---|---|
| Who is limited? | Per user / API key / IP / tenant |
| What is limited? | Requests per API, or globally |
| Limit? | e.g. 100 req / minute |
| Burst allowed? | Yes (token bucket) / No (hard window) |
| Response? | **429 Too Many Requests** + `Retry-After` |
| Scale? | Single gateway vs many gateway instances |
| Consistency? | Approximate OK vs strict global count |

**Functional:** enforce quota, return 429, fair per client  
**Non-functional:** low latency, highly available, works under traffic spikes

---

## 2. Where to put rate limiting

```text
CDN / WAF  →  API Gateway  →  Services
   ↑              ↑               ↑
 edge DDoS     best default    optional fine-grained
```

| Place | Pros | Cons |
|---|---|---|
| **API Gateway** | One place, before backends | Must be distributed if many GW pods |
| Each service | Fine-grained per API | Duplicated, easy to miss |
| CDN/WAF | Stops junk early | Less app-aware |

**Interview default:** Rate limit at **API Gateway** (after auth so you can key by user).

---

## 3. Algorithms (must know 3–4)

### A) Fixed window
- Count requests in calendar window (e.g. each minute `10:00–10:01`)  
- **Simple**  
- **Burst at boundary:** 100 at `10:00:59` + 100 at `10:01:00` = **~200 in 2 seconds**

#### Why “burst at boundary” happens (easy picture)

Limit = **100 requests per minute**. Windows are calendar slices:

```text
Window 1: 10:00:00 → 10:00:59   (own counter)
Window 2: 10:01:00 → 10:01:59   (counter resets to 0)
```

```text
10:00:59  →  send 100   (Window 1 full — allowed)
10:01:00  →  send 100   (Window 2 fresh — also allowed)
Total in ~2 seconds ≈ 200
```

Each window only saw 100, so the rule says OK — but the server felt a **200-request stampede**.

**Analogy:** cinema “100 tickets per hour”; at 10:59 take 100, at 11:00 take another 100 → stampede even though each hour looked fine.

**Our demo** (`api-gateway-auth-ratelimit`) uses this style for simplicity.

---

### B) Sliding window (log / counter)
- More accurate over **last 60 seconds** (rolling), not the clock minute  
- Needs more memory (timestamps) or smarter math  
- Smoother than fixed window  

#### How it fixes the boundary burst

It asks: *how many requests in the rolling last 60 seconds?* — **no hard reset at `:00`**.

```text
10:00:59  — already 100 in last 60s → 101st blocked
10:01:00  — still looks back ~60s; those 100 are still inside the window
            → you do NOT get a fresh 100
```

Slots free only as old requests age out of the lookback.

**Interview line:** “Quota is over a rolling time range, so the minute boundary doesn’t reset the count.”

---

### C) Token bucket (very popular)
- Bucket holds N tokens; refill at rate R  
- Each request consumes 1 token  
- Allows **controlled bursts**, steady average rate  

#### How it fixes the boundary burst

Tokens refill **continuously** — not “new minute = new full quota.”

```text
Bucket capacity = 100, refill ≈ 100/minute
Emptied at 10:00:59 (0 tokens)
At 10:01:00 you only get ~1–2 tokens refilled in that second
→ cannot dump another full 100 instantly
```

If the client was idle and the bucket is full, a **one-time burst up to capacity** is intentional. That is different from fixed window’s accidental **200 across two windows**.

**Interview line:** “Tokens refill gradually; emptying the bucket doesn’t unlock another full burst at the clock edge.”

---

### D) Leaky bucket
- Requests drain at fixed rate (queue)  
- Smooths traffic; excess dropped/queued  

---

### Algorithm pick (say in interview)

| Algorithm | Boundary spike? | Typical use |
|---|---|---|
| Fixed window | Yes (~2× near edge) | Demo / simple counters |
| Sliding window | No (rolling lookback) | Stricter fairness |
| Token bucket | No accidental double; controlled burst OK | **APIs (common choice)** |
| Leaky bucket | Smooths to fixed drain rate | Traffic shaping |

> “I’d pick **token bucket** for API rate limiting — supports burst and steady rate; fixed window is simpler but has boundary spikes. Sliding window also avoids the spike if we need stricter rolling accuracy.”

---
## 4. High-level design

### Single gateway instance
```text
Request → Auth (JWT) → RateLimiter(key=userId) → Route to service
                              │
                         allow / 429
```
Store: **in-memory** `ConcurrentHashMap` (demo only).

### Multiple gateway instances (production)
```text
GW1 \                    / Redis
GW2 —→ Rate limit check — 
GW3 /                    \
```
Store: **Redis** (shared counter / token bucket).

Why Redis?
- All GW pods share the same limit  
- Fast INCR / Lua scripts for atomicity  

---

## 5. Redis approaches (talking points)

| Approach | Idea |
|---|---|
| `INCR` + `EXPIRE` | Fixed window counter per key |
| Sorted set of timestamps | Sliding window |
| Token bucket in Lua | Atomic get/refill/consume |

**Key design examples**
```text
rl:user:{userId}:{route}:{yyyyMMddHHmm}
rl:ip:{ip}:global
rl:apikey:{key}:/api/orders
```

---

## 6. Request flow (with JWT)

```text
1. Client gets JWT (login / token endpoint)
2. Client calls API with Authorization: Bearer <jwt>
3. Gateway validates JWT → extract sub (userId)
4. Rate limit check for key = userId (or userId+path)
5a. Allowed → forward to Order/Product
5b. Blocked → 429 + Retry-After + JSON body
```

Auth first, then rate limit by **authenticated identity** (better than IP-only for APIs).

---

## 7. API response contract

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 30
Content-Type: application/json

{
  "success": false,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Try again later.",
  "limit": 5,
  "windowSeconds": 60
}
```

Optional headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

---

## 8. Edge cases / follow-ups

| Topic | Answer |
|---|---|
| Health/actuator | Exclude from rate limit |
| Login endpoint | Separate stricter limit (anti brute-force) |
| Burst | Token bucket |
| Distributed consistency | Redis + atomic ops |
| Bypass / premium users | Higher limits per plan |
| Clock skew | Prefer Redis server time / TTL |
| DDoS | CDN/WAF + gateway limits |

---

## 9. Rate limit vs related patterns

| Pattern | Difference |
|---|---|
| **Rate limiting** | Cap client request rate |
| **Throttle** | Similar; sometimes means slow-down vs hard reject |
| **Circuit Breaker** | Stop calling **unhealthy dependency** |
| **Bulkhead** | Isolate thread/connection pools |

---

## 10. 90-second system-design pitch

> “I’d put rate limiting at the API Gateway after JWT auth, keyed by user id. For algorithm I’d use token bucket to allow small bursts with a steady refill rate. For one instance, memory is fine; for many gateway pods I’d use Redis with atomic INCR or a Lua token-bucket script so limits are global. Exceeded clients get 429 with Retry-After. Health endpoints are excluded; login has a stricter limit.”

---

## 11. Our demo vs production

| | Demo (`api-gateway-auth-ratelimit`) | Production |
|---|---|---|
| Auth | JWT (HS256, demo secret) | OAuth2/JWT from IdP, JWKS |
| Limiter | In-memory fixed/token style | Redis + Gateway filter / custom |
| Scale | Single JVM | Multi-GW + shared store |

---

## 12. Quick whiteboard checklist

1. Requirements (who/what/limit/429)  
2. Placement (Gateway)  
3. Algorithm (Token bucket)  
4. Key = userId from JWT  
5. Single node vs Redis  
6. 429 contract  
7. Exceptions (health, tiers)  
