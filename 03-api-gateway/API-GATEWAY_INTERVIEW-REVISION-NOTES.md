# API Gateway — Interview Revision Notes

**Demo:** Spring Cloud Gateway `:8080` → Order `:8101`, Product `:8102`

---

## 1. What is API Gateway?

Single **entry point** for clients in front of many microservices.  
Handles routing and often cross-cutting concerns.

---

## 2. Why use it?

| Without gateway | With gateway |
|---|---|
| Client knows N service URLs | Client knows 1 URL |
| Auth duplicated everywhere | Auth at edge (common) |
| Hard to refactor internals | Internal ports/hosts hidden |

---

## 3. Gateway vs BFF vs Load Balancer

| Pattern | Role |
|---|---|
| **API Gateway** | Route + edge policies for APIs |
| **BFF** | Gateway-like API shaped per UI (mobile vs web) |
| **Load Balancer** | Distribute traffic to instances of *same* service |

Often: LB → Gateway → services (or Gateway with discovery client LB).

---

## 4. Common gateway responsibilities

- Routing / path rewrite  
- Authentication / authorization (JWT)  
- Rate limiting / throttling  
- Request logging / correlation IDs  
- SSL termination  
- Response aggregation (sometimes — careful of overuse)  

**Not usually:** heavy business logic (keep that in services).

---

## 5. Our demo mapping

| Client path | Backend |
|---|---|
| `/api/orders/**` | `order-backend/orders/**` |
| `/api/products/**` | `product-backend/products/**` |

Filters: rewrite path, add `X-Gateway`, global `X-Request-Id`.

---

## 6. `X-Request-Id` — significance (not authorization)

### Is request id authorization?
**No.**

| Concern | Purpose |
|---|---|
| Auth (`Authorization` / JWT) | Who is the user? Allow / deny |
| `X-Request-Id` | Which **request** is this? Trace it in logs |

Our demo has **routing + headers**; it does **not** implement login/JWT auth yet.

### Why do we need request id in the header?
So one client call can be **correlated** across Gateway → Product/Order logs.

```text
Call #1 → REQ-AAA → same id in gateway log + product log
Call #2 → REQ-BBB → separate trail
```

Without it, production incidents are hard to debug across services.

**Interview line:**  
> “Request/correlation ID is for observability/tracing, not security. We propagate it so every service logs the same id for one request.”

### Can client send their own request id?
**Yes.** Filter logic:
- If client sends `X-Request-Id` → Gateway **reuses** it  
- If missing/blank → Gateway **generates** `REQ-XXXXXXXX`

### Can we set it in properties like `X-Gateway`?
**Not for unique ids.**

```properties
# Static — same value on every request → OK for gateway name
AddRequestHeader=X-Gateway, api-gateway
```

```properties
# Wrong for tracing — every call would get the same id
AddRequestHeader=X-Request-Id, REQ-FIXED
```

| Header | `AddRequestHeader` in properties? | Why |
|---|---|---|
| `X-Gateway=api-gateway` | Yes | Constant marker |
| `X-Request-Id` | No (if unique per call) | Must be dynamic or client-provided → `RequestIdGlobalFilter` |

**One-liner:**  
> “Gateway name can be a static header in config; request id must be unique per call, so we use a GlobalFilter (reuse client id or generate one).”

---

## 6b. `requestId` + `traceId` (together, not confusing)

### Is `traceId` “under” `requestId`?
**No.** They are **two labels for the same client call**, side by side.  
**Spans** nest under **`traceId`** — not under `requestId`.

```text
One client API call
├── requestId  = REQ-AAA          ← support / business correlation
└── traceId    = 4bf92f...        ← distributed tracing id
       ├── span: API Gateway
       ├── span: Service 1
       │      ├── span: Service 2
       │      └── span: Service 3
       └── ...
```

| Relationship | Reality |
|---|---|
| Spans under `traceId` | Yes |
| `traceId` under `requestId` | **No** (not parent/child in tracing) |
| `requestId` ↔ `traceId` | Usually **1:1** for one API call |

**Analogy:** `requestId` = courier sticker number; `traceId` = GPS trip id with checkpoints (spans). Same delivery, two ids.

### Do we still need API Gateway if we have tracing?
**Yes — keep the Gateway.**  
Tracing does **not** replace routing/auth/rate-limit.  
Tracing may make a **custom** `X-Request-Id` optional as the *main* technical correlator; Gateway stays.

### Keeping both — will it confuse?
**No**, if roles are clear and logs show **both**:

```text
requestId=REQ-AAA traceId=4bf92f... spanId=... message=...
```

| Id | Purpose |
|---|---|
| `requestId` | Client/support-friendly key |
| `traceId` | Jaeger/Zipkin end-to-end graph + latency |

**Avoid:** overwriting `traceId` with `requestId`, or generating a new request id at every service.

### How to implement (high level)
1. **Gateway:** keep `RequestIdGlobalFilter` (client id or generate); enable Micrometer Tracing / OpenTelemetry  
2. **Services:** auto-instrument HTTP server + client (trace propagates hop-by-hop)  
3. **Optional:** put `X-Request-Id` in MDC once via filter (no `@RequestHeader` on every controller)  
4. **Support:** search logs by `requestId` → open matching `traceId` in tracing UI  

### Interview lines
> “requestId and traceId refer to the same request side-by-side. Spans nest under traceId; requestId is a parallel correlation key for support, not the parent of the trace.”

> “Tracing replaces custom request-id as the main hop-by-hop correlator if you want, but it does not replace the API Gateway.”

---

## 7. Spring Cloud Gateway basics (say in interview)

- Built on **WebFlux** (reactive), not servlet MVC  
- **Route** = id + uri + predicates + filters  
- **Predicate** = match rule (Path, Header, Method…)  
- **Filter** = modify request/response (pre/post)  
- **GlobalFilter** = applies to all routes  

---

## 8. Real-life analogy

**Hotel front desk**
- Guests don’t walk into kitchen/accounts directly  
- Front desk routes to restaurant / spa / billing  
- Checks ID (auth), gives token/badge (correlation / request id)  

---

## 9. Q&A

**Q: Gateway vs reverse proxy (Nginx)?**  
A: Overlap. Nginx often LB/SSL; Spring Cloud Gateway is app-aware routing/filters in Java ecosystem. Many use both.

**Q: Should every call go through gateway?**  
A: External clients yes. Internal service-to-service often talk directly or via mesh — don’t force all east-west through gateway.

**Q: Single point of failure?**  
A: Run gateway HA (multiple instances + LB). Still better than exposing all services.

**Q: Is X-Request-Id used for authorization?**  
A: No — tracing/correlation only. Auth is JWT/API key etc.

**Q: Why not AddRequestHeader for request id in properties?**  
A: That sets a fixed value. Request id must be unique per request (or taken from client) → GlobalFilter.

**Q: Is traceId under requestId?**  
A: No. Same call, two labels side-by-side. Spans nest under traceId; requestId is for support correlation.

**Q: If we use tracing, drop the API Gateway?**  
A: No. Keep Gateway for routing/edge concerns. Tracing observes the path; it doesn’t replace the gateway.

**Q: Next after gateway?**  
A: **Service Discovery** so gateway uses service names, not hardcoded hosts.

---

## 10. 90-second pitch

> “API Gateway is the single entry for clients. Mine uses Spring Cloud Gateway to route /api/orders and /api/products to separate backends, rewrite paths, and apply global filters for request IDs for tracing — not auth. Static headers like X-Gateway can be config; request id is dynamic via a GlobalFilter. In production I’d add JWT auth, rate limits, and discovery-based URIs.”
