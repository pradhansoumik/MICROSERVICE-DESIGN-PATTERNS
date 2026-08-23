# FLOW — JWT Auth + Rate Limit Gateway (:8085)

Shared backends: Order :8101, Product :8102

---

## A) Get token (public)

```text
Client
  │  POST /auth/token  {"username":"alice"}
  ▼
TokenController → JwtService.issueToken("alice")
  ▼
{ accessToken: "<jwt>", tokenType: "Bearer" }
```

No rate-limit / JWT required on `/auth/**`.

---

## B) API call (protected)

```text
Client
  │  GET /api/products
  │  Authorization: Bearer <jwt>
  ▼
JwtAuthGlobalFilter  (order -100)
  │  missing/invalid JWT → 401 JSON
  │  valid → attribute authenticatedUser = "alice"
  ▼
RateLimitGlobalFilter  (order -90)
  │  key = user:alice
  │  fixed window count++
  │  over limit → 429 JSON + Retry-After
  │  ok → continue
  ▼
Gateway route → product-backend:8102/products
  ▼
Response → Client
```

---

## C) Filter order (remember)

**This demo (`:8085`):**

```text
1) JWT auth     → who are you?
2) Rate limit   → how many calls for this user?
3) Route proxy  → forward to service
```

**Production note — not a replacement chain:**  
`RateLimitGlobalFilter` and `RequestIdGlobalFilter` are both `GlobalFilter`s but different jobs. Rate limit does **not** replace request-id. A full edge gateway typically runs **all** of them:

```text
RequestId  →  JwtAuth  →  RateLimit  →  route to backend
   │              │            │
   │              │            └─ throttle (can 429)
   │              └─ identity (can 401)
   └─ correlation only (never blocks)
```

| Filter | Purpose | Can block? |
|---|---|---|
| `RequestIdGlobalFilter` | Add/propagate `X-Request-Id` | No |
| `JwtAuthGlobalFilter` | Validate Bearer JWT | Yes → 401 |
| `RateLimitGlobalFilter` | Cap calls per user | Yes → 429 |

This secured demo skipped request-id to keep JWT + RL focused — add it back for real systems (see basic gateway `:8080`).

---

## D) Demo vs production

| | Demo | Production design |
|---|---|---|
| Auth | HS256 JWT on gateway | IdP + JWKS / OAuth2 |
| Rate limit store | In-memory map | Redis (multi-GW) |
| Algorithm | Fixed window | Token bucket / sliding window |
| Request id | Not in `:8085` demo | Keep `RequestId` + JWT + RL together |

See **DESIGN-RATE-LIMITING-SYSTEM.md** for full system-design talk.
