# Saga Choreography — Overview, Architecture & Interview Notes

**Status:** Notes only — **no code here yet**.  
**Implementation:** with **06 Event-Driven / Kafka** (after you finish Kafka learning).  
**Already coded:** Orchestration in `saga-demo` (`:8090`).

Compare also: `2PC-ORCHESTRATION-CHOREOGRAPHY.md` · orchestration demo: `README.md` / `FLOW.md`

---

## 1. Simple overview

**Choreography** = saga **without a central orchestrator**.  
Each service does a **local transaction**, then publishes a **domain event**. Other services **react** to events and continue (or compensate).

```text
Orchestration:   one boss calls Order → Inventory → Payment
Choreography:    Order emits event → Inventory listens → emits event → Payment listens …
```

Same saga goals:
- Local commits only (no 2PC across DBs)
- **Compensating** actions on failure
- **Eventual** consistency

Difference = **who owns the workflow** (events + handlers vs one orchestrator).

---

## 2. Architecture (Order → Inventory → Payment)

### Happy path

```text
Client
  │  place order
  ▼
Order Service
  │  create PENDING order (local DB)
  │  publish  OrderCreated
  ▼
        ┌──── Kafka topic: order-events ────┐
        ▼                                   │
Inventory Service                           │
  │  reserve stock (local DB)               │
  │  publish  InventoryReserved             │
  ▼                                         │
        ┌──── inventory-events ─────────────┤
        ▼                                   │
Payment Service                             │
  │  charge (local DB)                      │
  │  publish  PaymentCompleted              │
  ▼                                         │
Order Service (listens)                     │
  │  confirm order                          │
  └─────────────────────────────────────────┘
```

### Failure path (example: payment fails)

```text
Payment Service
  │  charge fails
  │  publish  PaymentFailed
  ▼
Inventory Service (listens)
  │  release stock   ← compensation
  │  publish  InventoryReleased (optional - if Order also listens to PaymentFailed)
  ▼
Order Service (listens)
  │  cancel order    ← compensation
```

No single class “runs” the saga — **event contracts** define the dance.

### Components you’ll need later (Kafka demo)

| Piece | Role |
|---|---|
| **Message broker** (Kafka) | Durable event transport |
| **Topics** | e.g. `order-events`, `inventory-events`, `payment-events` |
| **Producer** | After local commit, publish event |
| **Consumer** | Handle event → local TX → maybe publish next |
| **Idempotency** | Same event processed twice must be safe |
| **Outbox (prod)** | Don’t lose events if crash after DB commit |
| **Tracing** | Follow one saga across services (`traceId`) |

---

## 3. Orchestration vs Choreography (cheat sheet)

| | Orchestration (demo now) | Choreography (later) |
|---|---|---|
| Driver | Central orchestrator | Events + each service |
| Visibility | High | Lower without good tracing |
| Coupling | Orchestrator knows steps | Coupled via **event schemas** |
| Change flow | Mostly one place | Often several consumers |
| Failure handling | Orchestrator calls compensate | Services react to *Failed* events |
| Fits | Complex / money+stock flows | Simple event-native flows |

**For Order + Inventory + Payment:** interviews often prefer **orchestration** as default; say choreography when the platform is already Kafka-heavy and steps stay simple.

---

## 4. Interview notes

### Pitch (20–30 sec)

> “Saga choreography coordinates local transactions through domain events instead of a central orchestrator. Order publishes OrderCreated; Inventory reserves and publishes InventoryReserved; Payment charges and publishes completion or failure. On PaymentFailed, Inventory and Order compensate by releasing stock and cancelling. We’ll implement that with Kafka in the event-driven module; this repo’s saga demo uses orchestration for clarity.”

### Likely Q&As

**Q: Choreography vs orchestration?**  
A: Choreography = peers + events. Orchestration = central boss. Same compensations / eventual consistency.

**Q: Who triggers compensation?**  
A: A **failure event** (e.g. `PaymentFailed`); interested services compensate locally — no orchestrator method calling them.

**Q: Main risks?**  
A: Implicit workflow (hard to see), **duplicate events** (need idempotent consumers), **lost events** (outbox), harder debugging (need traces).

**Q: Why Kafka?**  
A: Durable, scalable pub/sub so multiple consumers can react and replay/retain events.

**Q: Still a saga?**  
A: Yes — multi-step local TXs + compensations across services; only the **coordination style** changes.

### One-liners to remember

- No central boss → **events are the protocol**.  
- **Publish after successful local commit** (or outbox).  
- Consumers must be **idempotent**.  
- Use **observability** (traceId) to see the saga path.

---

## 5. What you’ll implement later (checklist)

When Event-Driven / Kafka starts:

- [ ] Topics + event payloads (`OrderCreated`, `InventoryReserved`, `PaymentFailed`, …)  
- [ ] Order / Inventory / Payment as separate apps (or modules) producing/consuming  
- [ ] Happy path + payment-failure compensation path  
- [ ] Idempotency keys / processed-event store  
- [ ] `FLOW.md` + `DEMO.md` for choreography  

Until then: run **orchestration** `saga-demo` for hands-on saga practice.

---

## Related

| Doc | Content |
|---|---|
| `README.md` | Orchestration architecture (coded) |
| `FLOW.md` | Orchestration request path |
| `2PC-ORCHESTRATION-CHOREOGRAPHY.md` | 2PC vs both styles |
| `SAGA_INTERVIEW-REVISION-NOTES.md` | Broader saga Q&A |
| Future `06-event-driven/` | Choreography **code** |
