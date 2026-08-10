# Retry Demo — Simple Dummy API

Single Spring Boot app on **port 8083**.

## Idea

| Piece | Role |
|---|---|
| `FlakyApi` | Dummy dependency — fails first **2** calls, succeeds on **3rd** |
| `RetryCallerService` | Calls it with `@Retry` (maxAttempts=3, backoff) |
| `GET /api/demo/retry` | Endpoint you hit to see retries in logs |
| `GET /api/demo/retry-then-cb` | Retry then Circuit Breaker opens on sustained failure |
| `POST /api/payments/charge` | Dummy payment with **Idempotency-Key** (no double charge) |
| `POST /api/payments/charge-unsafe` | Same without key (shows double charge risk) |

## Run

```powershell
cd D:\planning-preparation-Execution\MICROSERVICES_NOTES\microservice-design-patterns\01-circuit-breaker\retry-demo
mvn spring-boot:run
```

## Try it

### 1) Happy retry (fail, fail, success)
```powershell
# Reset counter first (important if you already called before)
Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/flaky/reset

Invoke-RestMethod http://localhost:8083/api/demo/retry
```

**Console logs should show:**
```text
FlakyApi attempt #1  → TransientApiException
(wait ~500ms)
FlakyApi attempt #2  → TransientApiException
(wait ~1000ms exponential)
FlakyApi attempt #3  → SUCCESS
```

**Response:** `outcome = SUCCESS_AFTER_RETRY_OR_FIRST_TRY`, `usedFallback = false`

### 2) Exhaust retries (make it always fail)
In `application.properties` set:
```properties
demo.flaky.fail-times=10
```
Restart, reset, call `/api/demo/retry` → after 3 attempts → fallback `FAILED_AFTER_RETRIES`

### 3) Business error — no retry
```powershell
Invoke-RestMethod http://localhost:8083/api/demo/business
```
Logs: **only 1 attempt** (BusinessApiException is not in `retryExceptions`)

### 4) Direct flaky call (no Retry)
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/flaky/reset
Invoke-RestMethod http://localhost:8083/api/flaky
```
First call throws 500 — proves Retry layer is what saves `/demo/retry`.

### 5) Retry then Circuit Breaker (new)

**Goal:** show Retry handling blips first; after sustained failure CB opens and fail-fast takes over.

```text
HTTP call (CLOSED):  AlwaysFail ×3 (Retry) → RETRY_EXHAUSTED_FALLBACK
                     (failures counted by CB)
more HTTP calls   →  failure rate ≥ 50% → state OPEN
next HTTP call    →  CIRCUIT_OPEN_FALLBACK (no AlwaysFailApi attempts in logs)
```

**Steps:**
```powershell
# Reset CB + counters
Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/demo/retry-then-cb/reset

# Call 1 — watch logs: AlwaysFailApi attempt #1 #2 #3
Invoke-RestMethod http://localhost:8083/api/demo/retry-then-cb

# Call 2 / 3 — keep calling until circuit.state becomes OPEN
Invoke-RestMethod http://localhost:8083/api/demo/retry-then-cb
Invoke-RestMethod http://localhost:8083/api/demo/retry-then-cb

# Status only
Invoke-RestMethod http://localhost:8083/api/demo/retry-then-cb/status
```

**What to look for:**
| Phase | Response `outcome` | Logs |
|---|---|---|
| Still CLOSED | `RETRY_EXHAUSTED_FALLBACK` | 3× `AlwaysFailApi attempt` |
| OPEN | `CIRCUIT_OPEN_FALLBACK` | **No** new AlwaysFailApi attempts |

Also check `circuit.state` in the JSON (`CLOSED` → `OPEN`).

**Interview line:**  
> "Retry absorbs short blips. When failures continue, Circuit Breaker opens and we fail fast — retries no longer hammer the dead dependency."

## Config used (Retry only)

```properties
resilience4j.retry.instances.flakyApi.maxAttempts=3
resilience4j.retry.instances.flakyApi.waitDuration=500ms
resilience4j.retry.instances.flakyApi.enableExponentialBackoff=true
resilience4j.retry.instances.flakyApi.exponentialBackoffMultiplier=2
resilience4j.retry.instances.flakyApi.retryExceptions[0]=com.interview.retry.TransientApiException
```

## Interview line (Retry)

> "I hit a flaky dummy API that fails twice then succeeds. With Resilience4j Retry maxAttempts=3 and exponential backoff, the client recovers automatically. Business exceptions are excluded from retryExceptions so we don't retry insufficient-funds style errors."

---

## Idempotency-Key demo (dummy payment API)

**Problem:** Client times out after payment succeeded → retries → **double charge** without idempotency.

**Solution:** Client sends `Idempotency-Key`. Server stores first result; replay returns same `paymentId` and does **not** charge again.

| Endpoint | Behavior |
|---|---|
| `POST /api/payments/charge` + header `Idempotency-Key` | Safe — charge once per key |
| `POST /api/payments/charge-unsafe` | Unsafe — every call charges |
| `GET /api/payments/stats` | `httpCallsReceived` vs `chargesExecuted` |
| `POST /api/payments/reset` | Clear store/counters |

### Safe flow (same key twice)
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/payments/reset

$body = '{"orderId":"ORD-1","customerId":"CUST-1","amount":999}'
$headers = @{ "Idempotency-Key" = "ORD-1"; "Content-Type" = "application/json" }

# Call 1 — real charge
Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/payments/charge -Headers $headers -Body $body

# Call 2 — simulated retry with SAME key
Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/payments/charge -Headers $headers -Body $body

Invoke-RestMethod http://localhost:8083/api/payments/stats
```

**Expect:**
- 1st response: `replayed=false`, new `paymentId`
- 2nd response: `replayed=true`, **same** `paymentId`, message says not charged again
- stats: `httpCallsReceived=2`, `chargesExecuted=1`

### Unsafe flow (double charge)
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/payments/reset
$body = '{"orderId":"ORD-2","customerId":"CUST-1","amount":999}'

Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/payments/charge-unsafe -ContentType "application/json" -Body $body
Invoke-RestMethod -Method POST -Uri http://localhost:8083/api/payments/charge-unsafe -ContentType "application/json" -Body $body
Invoke-RestMethod http://localhost:8083/api/payments/stats
```

**Expect:** two different `paymentId`s, `chargesExecuted=2` ← this is the bug Retry can cause.

### Interview line (Idempotency)

> "Retry without idempotency can double-charge. We send an Idempotency-Key; the server stores the first result and replays it on duplicate requests so the side-effect runs only once."
