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
| 03 | API Gateway | ⏳ Next | Single entry for Order/Payment/Inventory |
| 05 | Service Discovery | ⏳ Planned | Eureka / K8s DNS style discovery |
| 06 | Event-Driven (+ Saga choreography) | ⏳ Planned | Order placed → Kafka → Inventory/Notification |
| 07 | Observability | ⏳ Planned | Metrics, tracing, health, logs |

> Study order adjusted: **CQRS before API Gateway** (both are independent; CQRS is a data pattern).

## Pattern 01 — Circuit Breaker

| Item | Path | Purpose |
|---|---|---|
| CB demo apps | `01-circuit-breaker/payment-service` + `order-service` | CB + TimeLimiter + fallback |
| CB run sheet | `01-circuit-breaker/DEMO.md` | PowerShell commands |
| CB deep notes | `01-circuit-breaker/CB_INTERVIEW-REVISION-NOTES.md` | States, config, SLOW/FAIL, TimeLimiter |
| **CB internals** | `01-circuit-breaker/CB-INTERNAL-WORKING.md` | How CB works internally (interview) |
| Retry demo | `01-circuit-breaker/retry-demo` | Retry, Retry→CB, Idempotency |
| CB architecture | `01-circuit-breaker/README.md` | Diagrams + pitch |
| **FLOW** | `01-circuit-breaker/FLOW.md` | Controller → CB → Payment |

## Pattern 02 — Saga

| Item | Path | Purpose |
|---|---|---|
| Saga demo app | `02-saga/saga-demo` (:8090) | Orchestration: Order→Inventory→Payment |
| Demo sheet | `02-saga/DEMO.md` | Happy path + payment/inventory fail |
| Interview notes | `02-saga/SAGA_INTERVIEW-REVISION-NOTES.md` | Orchestration vs choreography |
| 2PC / Orch / Choreo | `02-saga/2PC-ORCHESTRATION-CHOREOGRAPHY.md` | Why Saga, 2PC, orchestration vs choreography, compensate txns |
| Architecture | `02-saga/README.md` | Diagrams + pitch |
| **FLOW** | `02-saga/FLOW.md` | Controller → orchestrator → compensate |

## Pattern 04 — CQRS

| Item | Path | Purpose |
|---|---|---|
| CQRS demo | `04-cqrs/cqrs-demo` (:8091) | Commands vs queries + projection |
| Demo sheet | `04-cqrs/DEMO.md` | Place → query → cancel → query |
| Interview notes | `04-cqrs/CQRS_INTERVIEW-REVISION-NOTES.md` | Why CQRS, vs Event Sourcing |
| Flow (controller→service) | `04-cqrs/CQRS-FLOW-CONTROLLER-TO-SERVICE.md` | Detailed flow note |
| **FLOW** | `04-cqrs/FLOW.md` | Standard flow doc (commands/queries/projector) |
| Architecture | `04-cqrs/README.md` | Diagrams + pitch |

## Ports

| App | Port |
|---|---|
| payment-service (CB) | 8081 |
| order-service (CB) | 8082 |
| retry-demo | 8083 |
| saga-demo | 8090 |
| cqrs-demo | **8091** |

## Prerequisites

- JDK 17+
- Maven 3.9+
- Free ports as needed

## How to use

1. Read `README.md` → run `DEMO.md` → revise interview notes  
2. Always read **`FLOW.md`** (controller → services → key hooks)  
3. Practice 90-second pitch  

**Convention:** every pattern folder includes a **`FLOW.md`**.

**Next:** **03 — API Gateway** (after you finish CQRS practice)