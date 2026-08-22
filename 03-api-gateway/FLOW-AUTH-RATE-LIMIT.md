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

```text
1) JWT auth     → who are you?
2) Rate limit   → how many calls for this user?
3) Route proxy  → forward to service
```

---

## D) Demo vs production

| | Demo | Production design |
|---|---|---|
| Auth | HS256 JWT on gateway | IdP + JWKS / OAuth2 |
| Rate limit store | In-memory map | Redis (multi-GW) |
| Algorithm | Fixed window | Token bucket / sliding window |

See **DESIGN-RATE-LIMITING-SYSTEM.md** for full system-design talk.
