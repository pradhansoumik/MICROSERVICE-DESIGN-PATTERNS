# Microservice Design Patterns — Interview Prep (Java + Spring Boot)

Hands-on demos with **real-life examples**, runnable Spring Boot code, and architecture diagrams you can explain in interviews.

**Location:** `D:\planning-preparation-Execution\MICROSERVICES_NOTES\GITHUB_CHECKIN\MICROSERVICE-DESIGN-PATTERNS`  
**GitHub:** https://github.com/pradhansoumik/MICROSERVICE-DESIGN-PATTERNS

## Roadmap

| # | Pattern | Status | Real-life example |
|---|---|---|---|
| 01 | **Circuit Breaker** (+ Retry, Idempotency, TimeLimiter) | ✅ Done | Order → Payment; flaky API; idempotent charge |
| 02 | **Saga** | ✅ Done | Order → Inventory → Payment + compensation |
| 04 | **CQRS** | ✅ Done | Order write model vs read model |
| 03 | **API Gateway** | ✅ Done | Single entry → Order + Product backends |
| 05 | Service Discovery | ⏳ Next | Eureka / K8s DNS style discovery |
| 06 | Event-Driven (+ Saga choreography) | ⏳ Planned | Order placed → Kafka → Inventory/Notification |
| 07 | Observability | ⏳ Planned | Metrics, tracing, health, logs |

## Pattern 01 — Circuit Breaker

| Item | Path | Purpose |
|---|---|---|
| CB demo apps | `01-circuit-breaker/payment-service` + `order-service` | CB + TimeLimiter + fallback |
| **CB internals** | `01-circuit-breaker/CB-INTERNAL-WORKING.md` | How CB / `@CircuitBreaker` works |
| **FLOW** | `01-circuit-breaker/FLOW.md` | Controller → CB → Payment |

## Pattern 02 — Saga

| Item | Path | Purpose |
|---|---|---|
| Saga demo | `02-saga/saga-demo` (:8090) | Orchestration + compensate |
| **FLOW** | `02-saga/FLOW.md` | Controller → orchestrator → compensate |

## Pattern 04 — CQRS

| Item | Path | Purpose |
|---|---|---|
| CQRS demo | `04-cqrs/cqrs-demo` (:8091) | Commands vs queries + projection |
| **FLOW** | `04-cqrs/FLOW.md` | Command → event → projector → query |

## Pattern 03 — API Gateway

| Item | Path | Purpose |
|---|---|---|
| Basic gateway | `03-api-gateway/api-gateway` (:8080) | Routing + filters |
| Shared backends | `order-backend` (:8101), `product-backend` (:8102) | Common services |
| **Auth + Rate Limit GW** | `03-api-gateway/api-gateway-auth-ratelimit` (:8085) | JWT + in-memory RL |
| **Design RL system** | `03-api-gateway/DESIGN-RATE-LIMITING-SYSTEM.md` | Interview system design |
| Auth notes | `03-api-gateway/AUTH-JWT-NOTES.md` | JWT at edge |
| Demo / Flow (secure) | `DEMO-AUTH-RATE-LIMIT.md`, `FLOW-AUTH-RATE-LIMIT.md` | 401 / 429 practice |
| **FLOW** (basic) | `03-api-gateway/FLOW.md` | Client → gateway → backend |

## Ports

| App | Port |
|---|---|
| **api-gateway** | **8080** |
| **api-gateway-auth-ratelimit** | **8085** |
| payment-service (CB) | 8081 |
| order-service (CB) | 8082 |
| retry-demo | 8083 |
| saga-demo | 8090 |
| cqrs-demo | 8091 |
| order-backend (GW) | 8101 |
| product-backend (GW) | 8102 |

## How to use

1. Read pattern `README.md` → `FLOW.md` → run `DEMO.md`  
2. Revise interview notes / pitch  
3. **Convention:** every pattern has a **`FLOW.md`**

**Next:** **05 — Service Discovery**
