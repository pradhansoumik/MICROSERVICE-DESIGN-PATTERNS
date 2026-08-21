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

## 6. Spring Cloud Gateway basics (say in interview)

- Built on **WebFlux** (reactive), not servlet MVC  
- **Route** = id + uri + predicates + filters  
- **Predicate** = match rule (Path, Header, Method…)  
- **Filter** = modify request/response (pre/post)  
- **GlobalFilter** = applies to all routes  

---

## 7. Real-life analogy

**Hotel front desk**
- Guests don’t walk into kitchen/accounts directly  
- Front desk routes to restaurant / spa / billing  
- Checks ID (auth), gives token/badge (correlation)  

---

## 8. Q&A

**Q: Gateway vs reverse proxy (Nginx)?**  
A: Overlap. Nginx often LB/SSL; Spring Cloud Gateway is app-aware routing/filters in Java ecosystem. Many use both.

**Q: Should every call go through gateway?**  
A: External clients yes. Internal service-to-service often talk directly or via mesh — don’t force all east-west through gateway.

**Q: Single point of failure?**  
A: Run gateway HA (multiple instances + LB). Still better than exposing all services.

**Q: Next after gateway?**  
A: **Service Discovery** so gateway uses service names, not hardcoded hosts.

---

## 9. 90-second pitch

> “API Gateway is the single entry for clients. Mine uses Spring Cloud Gateway to route /api/orders and /api/products to separate backends, rewrite paths, and apply global filters for request IDs. That hides internal topology and centralizes cross-cutting concerns. In production I’d add JWT auth, rate limits, and discovery-based URIs.”
