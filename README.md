# Microservice Design Patterns — Interview Prep (Java + Spring Boot)

Hands-on demos with **real-life examples**, runnable Spring Boot code, and architecture diagrams you can explain in interviews.

**Location:** `D:\planning-preparation-Execution\MICROSERVICES_NOTES\GITHUB_CHECKIN\MICROSERVICE-DESIGN-PATTERNS`  
**GitHub:** https://github.com/pradhansoumik/MICROSERVICE-DESIGN-PATTERNS

## Roadmap

| # | Pattern | Status | Real-life example |
|---|---|---|---|
| 01 | **Circuit Breaker** (+ Retry, Idempotency, TimeLimiter) | ✅ Done | Order → Payment; flaky API; idempotent charge |
| 02 | **Saga** | ✅ Done | Order → Inventory → Payment + compensation |
| 03 | API Gateway | ⏳ Planned | Single entry for Order/Payment/Inventory |
| 04 | CQRS | ⏳ Planned | Order write vs Order read model |
| 05 | Service Discovery | ⏳ Planned | Eureka / K8s DNS style discovery |
| 06 | Event-Driven | ⏳ Planned | Order placed → Kafka → Inventory/Notification |
| 07 | Observability | ⏳ Planned | Metrics, tracing, health, logs |

## Pattern 01 — Circuit Breaker

| Item | Path | Purpose |
|---|---|---|
| CB demo apps | `01-circuit-breaker/payment-service` + `order-service` | CB + TimeLimiter + fallback |
| CB run sheet | `01-circuit-breaker/DEMO.md` | PowerShell commands |
| CB deep notes | `01-circuit-breaker/CB_INTERVIEW-REVISION-NOTES.md` | States, config, SLOW/FAIL, TimeLimiter |
| Retry demo | `01-circuit-breaker/retry-demo` | Retry, Retry→CB, Idempotency |
| Retry vs CB | `01-circuit-breaker/RETRY-VS-CB.md` | Comparison + real-life analogy |

## Pattern 02 — Saga

| Item | Path | Purpose |
|---|---|---|
| Saga demo app | `02-saga/saga-demo` (:8090) | Orchestration: Order→Inventory→Payment |
| Demo sheet | `02-saga/DEMO.md` | Happy path + payment/inventory fail |
| Interview notes | `02-saga/SAGA_INTERVIEW-REVISION-NOTES.md` | Orchestration vs choreography |
| 2PC / Orch / Choreo | `02-saga/2PC-ORCHESTRATION-CHOREOGRAPHY.md` | Why Saga, 2PC, orchestration vs choreography, compensate txns |
| Architecture | `02-saga/README.md` | Diagrams + pitch |

## Ports

| App | Port |
|---|---|
| payment-service (CB) | 8081 |
| order-service (CB) | 8082 |
| retry-demo | 8083 |
| saga-demo | **8090** |

## Prerequisites

- JDK 17+
- Maven 3.9+
- Free ports as needed

## How to use

1. Read notes → run demos → practice 90-second pitch
2. Next pattern: **03 — API Gateway**
