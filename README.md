# Microservice Design Patterns — Interview Prep (Java + Spring Boot)

Hands-on demos with **real-life examples**, runnable Spring Boot code, and architecture diagrams you can explain in interviews.

**Location:** `D:\planning-preparation-Execution\MICROSERVICES_NOTES\microservice-design-patterns`

## Roadmap

| # | Pattern | Status | Real-life example |
|---|---|---|---|
| 01 | **Circuit Breaker** (+ Retry, Idempotency, TimeLimiter) | ✅ Done | Order → Payment; flaky API; idempotent charge |
| 02 | Saga | ⏳ Next | Order → Payment → Inventory (distributed txn) |
| 03 | API Gateway | ⏳ Planned | Single entry for Order/Payment/Inventory |
| 04 | CQRS | ⏳ Planned | Order write vs Order read model |
| 05 | Service Discovery | ⏳ Planned | Eureka / K8s DNS style discovery |
| 06 | Event-Driven | ⏳ Planned | Order placed → Kafka → Inventory/Notification |
| 07 | Observability | ⏳ Planned | Metrics, tracing, health, logs |

## Pattern 01 — what you have

| Item | Path | Purpose |
|---|---|---|
| CB demo apps | `01-circuit-breaker/payment-service` + `order-service` | CB + TimeLimiter + fallback |
| CB run sheet | `01-circuit-breaker/DEMO.md` | PowerShell commands |
| CB deep notes | `01-circuit-breaker/CB_INTERVIEW-REVISION-NOTES.md` | States, config, SLOW/FAIL, TimeLimiter |
| CB architecture | `01-circuit-breaker/README.md` | Diagrams + pitch |
| Retry demo | `01-circuit-breaker/retry-demo` | Retry, Retry→CB, Idempotency |
| Retry notes | `01-circuit-breaker/RETRY-PATTERN-NOTES.md` | Retry rules + config |
| Retry vs CB | `01-circuit-breaker/RETRY-VS-CB.md` | Comparison + real-life analogy |

## Ports

| App | Port |
|---|---|
| payment-service | 8081 |
| order-service | 8082 |
| retry-demo | 8083 |

## Prerequisites

- JDK 17+
- Maven 3.9+
- Free ports 8081–8083

## How to use

1. Read notes → run demos → practice 90-second pitch
2. When ready, start **02 — Saga**
