# API Gateway — FLOW

Demo ports: Gateway **8080**, Order **8101**, Product **8102**

---

## A) Happy path — list products

```text
Client
  │  GET http://localhost:8080/api/products
  ▼
API Gateway (Spring Cloud Gateway)
  │  1) GlobalFilter RequestIdGlobalFilter
  │       - ensure X-Request-Id header
  │  2) Match route: Path=/api/products/**
  │  3) Filters:
  │       - RewritePath → /products
  │       - AddRequestHeader X-Gateway=api-gateway
  │  4) Forward to http://localhost:8102
  ▼
product-backend ProductController
  │  GET /products
  │  reads headers X-Request-Id, X-Gateway
  ▼
Response back through Gateway → Client
```

---

## B) Create order path

```text
Client
  │  POST /api/orders  { customerId, productId, amount }
  ▼
API Gateway
  │  match order-route
  │  rewrite /api/orders → /orders
  │  add X-Gateway
  ▼
order-backend OrderController
  │  POST /orders → create ORD-xxxx
  ▼
Client gets CREATED response
```

---

## C) What runs where

| Layer | Component | Job |
|---|---|---|
| Edge | `api-gateway` | Single entry, route, rewrite, headers |
| Global filter | `RequestIdGlobalFilter` | Correlation id for every request |
| Error filter | `StructuredErrorGlobalFilter` | On downstream failure → structured 503 JSON |
| Route config | `application.properties` | Predicates + filters + URI |
| Downstream | `order-backend` | Order APIs |
| Downstream | `product-backend` | Product APIs |

---

## D) Error path (filter chain — not Circuit Breaker)

```text
Client → Gateway → backend DOWN / connection error
                         │
                         ▼
              filter chain Mono errors
                         │
                         ▼
         StructuredErrorGlobalFilter.onErrorResume
                         │
                         ▼
         HTTP 503 JSON { success:false, source:api-gateway, ... }
                         │
                         ▼
                      Client
```

---

## E) Interview line

> “Client hits only the gateway. Gateway matches a route by path, rewrites to the internal path, adds cross-cutting headers via filters, and proxies to the correct backend. If the downstream call fails, a GlobalFilter catches the error on the filter chain and returns a structured JSON error — the client still only talks to the gateway.”

**Production note:** Use Eureka/K8s DNS / Consul for dynamic `uri` instead of `http://localhost:8101`. Rate limiting/auth often sit at the gateway too.
