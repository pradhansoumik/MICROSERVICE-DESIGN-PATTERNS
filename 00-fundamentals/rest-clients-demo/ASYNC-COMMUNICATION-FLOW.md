# Async Communication — Flow Understanding

**Related:** sync REST/Feign demos in this folder · Saga choreography `../../02-saga/CHOREOGRAPHY.md` · full Kafka code later in `06-event-driven`

---

## 1. Sync vs Async (picture)

### Synchronous (what RestTemplate / RestClient / Feign do)

```text
Order Service                     Inventory Service
     │                                    │
     │ ──── HTTP request ───────────────► │
     │         (waiting…)                 │ work
     │ ◄──── HTTP response ────────────── │
     │  continue                          │
```

Caller **blocks** until response (or timeout).

### Asynchronous (events / messaging)

```text
Order Service                Kafka / broker              Inventory   Notification
     │                            │                          │            │
     │ ── publish OrderCreated ─► │                          │            │
     │  continue (no wait)        │ ── push / pull ────────► │            │
     │                            │ ── push / pull ─────────────────────► │
```

Publisher **does not wait** for consumers. Consumers work **independently** (often in parallel).

---

## 2. End-to-end async flow (happy path)

Example: place order → reserve stock → notify user

```text
1) Client → Order API (sync HTTP is fine at the edge)
2) Order Service
     - saves order (local DB)
     - publishes event: OrderCreated { orderId, items… }
3) Broker (Kafka topic: order-events)
4a) Inventory Service consumes OrderCreated
      - reserves stock
      - may publish InventoryReserved
4b) Notification Service consumes OrderCreated
      - sends email/SMS
```

```text
   Client
     │  POST /orders   ← still often SYNC for “accepted”
     ▼
 Order Service ──► [ Kafka: order-events ] ──┬──► Inventory
                     OrderCreated            └──► Notification
```

**Key idea:** edge call can be sync (“202 Accepted”); **internal fan-out** is async.

---

## 3. Failure / compensation flow (async saga style)

```text
Payment fails after stock reserved
        │
        ▼
Payment publishes PaymentFailed
        │
        ├──► Inventory consumes → release stock
        └──► Order consumes     → cancel order
```

No central orchestrator required (choreography). Details: `02-saga/CHOREOGRAPHY.md`.

---

## 4. Who does what

| Component | Role |
|---|---|
| **Producer** | After local commit, **publish** event |
| **Broker** (Kafka) | Stores/distributes events |
| **Consumer group** | One or more services process events |
| **Idempotency** | Same event twice must be safe |
| **DLQ** | Bad messages don’t block forever |
| **Outbox (prod)** | Don’t lose event if crash after DB commit |

---

## 5. When interviewers ask “how do services communicate?”

| Style | Answer |
|---|---|
| Sync | REST/gRPC — RestClient/Feign — user waits |
| Async | Kafka events — OrderCreated → many consumers |
| Mix | Sync at API edge; async for workflows & fan-out |

---

## 6. This folder vs later demo

| Now | Later |
|---|---|
| This note = **flow understanding** | `06-event-driven` = runnable Kafka + choreography |
| REST demos = sync only | Async needs a broker |

---

## Interview one-liner

> “Async communication uses a broker: a service publishes an event after its local commit; other services consume and react. The producer doesn’t wait. We use it for fan-out and sagas; we still use sync REST/gRPC when the caller needs an immediate response.”
