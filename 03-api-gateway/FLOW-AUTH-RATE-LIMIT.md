# FLOW — JWT Auth + Rate Limit Gateway (:8085)

Shared backends: Order :8101, Product :8102  
System-design talk (algorithms, Redis, 429 contract) → **`DESIGN-RATE-LIMITING-SYSTEM.md`**

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

## B) API call — short path (demo)

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
Route + RewritePath + Netty proxy → product-backend:8102/products
  ▼
Response → Client
```

---

## C) Request & response flow (layers + filters)

### C.1 Big picture — layers

Request goes **in** left → right; response comes **back** right → left.

```text
 CLIENT
   │  HTTP request (+ Authorization: Bearer jwt)
   ▼
┌──────────────────────────────────────────────────────────┐
│  CDN / WAF (optional)                                    │
│  • DDoS / bot filter, TLS terminate sometimes            │
│  • May apply coarse IP rate limit                        │
└────────────────────────────┬─────────────────────────────┘
                             ▼
┌──────────────────────────────────────────────────────────┐
│  API GATEWAY  (best place for app rate limiting)         │
│                                                          │
│  Custom GlobalFilters (your code)                        │
│    1. RequestId   → correlation (never blocks)           │
│    2. JWT Auth    → who? (can 401)                       │
│    3. Rate Limit  → Redis/memory by userId (can 429)     │
│                                                          │
│  Built-in Gateway routing (NOT RateLimit filter)         │
│    4. Route match      → Path predicate picks Route      │
│    5. RewritePath      → fix path for backend            │
│    6. RouteToRequestUrl → build final http(s) URL        │
│    7. NettyRouting     → PROXY call to backend           │
│                                                          │
│  If 401/429 in steps 2–3 → response to client;           │
│  steps 4–7 never run (backend NOT called).               │
└────────────────────────────┬─────────────────────────────┘
                             │ allow + proxy
                             ▼
┌──────────────────────────────────────────────────────────┐
│  DOWNSTREAM SERVICE  (Order :8101 / Product :8102)       │
│  • Business logic / DB                                   │
└────────────────────────────┬─────────────────────────────┘
                             │ HTTP response (200/4xx/5xx)
                             ▼
              NettyWriteResponseFilter (Gateway)
                             │
                    CDN/WAF (if any)
                             │
                          CLIENT
```

**Interview line:** “Auth and rate limit at the gateway so bad traffic never reaches services. Routing/rewrite/proxy are separate built-in Gateway filters after RL allows the call.”

---

### C.2 Gateway filter chain — request path (detail)

```text
Request enters Gateway
        │
        ▼
 [RequestIdGlobalFilter]      add/propagate X-Request-Id
        │                       (demo :8085 skips this; production keep it)
        ▼
 [JwtAuthGlobalFilter]        no/invalid Bearer? ──► 401 JSON ──► Client (stop)
        │ OK
        │ store userId (JWT sub) on exchange
        ▼
 [RateLimitGlobalFilter]      over quota? ──► 429 + Retry-After ──► Client (stop)
        │ OK                    (memory demo / Redis prod)
        │ add X-RateLimit-* headers
        │
        │  >>> RateLimit does NOT route / rewrite / proxy <<<
        ▼
 [RoutePredicateHandlerMapping]
        Path=/api/products/** → select Route (uri=http://localhost:8102)
        │
        ▼
 [RewritePath GatewayFilter]  RewritePathGatewayFilterFactory
        /api/products → /products   (from application.properties)
        │
        ▼
 [RouteToRequestUrlFilter]    built-in GlobalFilter
        final URL = http://localhost:8102/products
        │
        ▼
 [NettyRoutingFilter]         built-in GlobalFilter — actual PROXY HTTP call
        │
        ▼
 Downstream service response
        │
        ▼
 [NettyWriteResponseFilter]   write backend response to Client
```

| Step | Component | Custom or built-in? | Success | Failure |
|---|---|---|---|---|
| 1 | `RequestIdGlobalFilter` | Custom (basic GW `:8080`) | continue | — |
| 2 | `JwtAuthGlobalFilter` | Custom | continue with `userId` | **401** |
| 3 | `RateLimitGlobalFilter` | Custom | continue | **429** |
| 4 | `RoutePredicateHandlerMapping` | Built-in | Route selected | 404 if no match |
| 5 | `RewritePath` (`RewritePathGatewayFilterFactory`) | Built-in route filter | path fixed | — |
| 6 | `RouteToRequestUrlFilter` | Built-in GlobalFilter | target URL set | — |
| 7 | `NettyRoutingFilter` | Built-in GlobalFilter | proxied to backend | connection/5xx |
| 8 | `NettyWriteResponseFilter` | Built-in GlobalFilter | client gets body | — |

#### Who does route / rewrite / proxy?

| Job | Responsible | In `RateLimitGlobalFilter`? |
|---|---|---|
| **Match route** | `RoutePredicateHandlerMapping` + route `predicates` in properties | **No** |
| **Rewrite path** | `RewritePath` → `RewritePathGatewayFilterFactory` | **No** |
| **Build target URL** | `RouteToRequestUrlFilter` | **No** |
| **Proxy HTTP call** | `NettyRoutingFilter` | **No** |
| **Write response** | `NettyWriteResponseFilter` | **No** |

Configured in properties (example), not in RateLimit Java code:

```properties
spring.cloud.gateway.routes[1].uri=http://localhost:8102
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/products/**
spring.cloud.gateway.routes[1].filters[0]=RewritePath=/api/products(?<segment>/?.*), /products$\{segment}
```

---

### C.3 Response path (what client sees)

```text
A) Blocked at auth
   Client ← 401 UNAUTHORIZED  (from JwtAuthGlobalFilter)

B) Blocked at rate limit
   Client ← 429 RATE_LIMIT_EXCEEDED
            + Retry-After
            + X-RateLimit-Limit / Remaining  (from RateLimitGlobalFilter)
            (NettyRoutingFilter never runs)

C) Allowed
   Client ← NettyWriteResponseFilter ← downstream status/body
            (+ rate-limit headers may already be on the response)
```

Response does **not** re-run auth/RL; those decided on the way in. On allow, Gateway proxies via `NettyRoutingFilter` / `NettyWriteResponseFilter`.

---

### C.4 Demo chain (`:8085`) vs production

**This demo:**

```text
JwtAuthGlobalFilter (-100) → RateLimitGlobalFilter (-90)
  → Route match + RewritePath → RouteToRequestUrlFilter + NettyRoutingFilter
  → NettyWriteResponseFilter → Client
```

**Production filter order (remember):** Rate limit does **not** replace request-id.

```text
RequestId  →  JwtAuth  →  RateLimit  →  route / rewrite / proxy
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

`:8085` skipped request-id to keep JWT + RL focused — add it back for real systems (see basic gateway `:8080`).

Public paths (`/auth/**`, `/actuator/**`) skip the protected JWT+RL path.

Auth first, then rate limit by **authenticated identity** (better than IP-only for APIs).

---

## D) Demo vs production

| | Demo | Production design |
|---|---|---|
| Auth | HS256 JWT on gateway | IdP + JWKS / OAuth2 |
| Rate limit store | In-memory map | Redis (multi-GW) |
| Algorithm | Fixed window | Token bucket / sliding window |
| Request id | Not in `:8085` demo | Keep `RequestId` + JWT + RL together |

See **DESIGN-RATE-LIMITING-SYSTEM.md** for algorithms, Redis keys, 429 contract, interview pitch.
