# Saga — Interview Revision Notes

**Demo:** `02-saga/saga-demo` on port **8090** (orchestration)  
**Story:** Place Order → Reserve Inventory → Charge Payment → Confirm Order

---

## 1. Why Saga?

Microservices = **Database per service** → no single ACID transaction across Order + Inventory + Payment.

| Approach | Verdict |
|---|---|
| 2PC / XA distributed txn | Slow, brittle, rarely used in modern MS |
| **Saga** | Local txns + compensations → **eventual consistency** |

---

## 2. Definition

Saga = sequence of **local transactions**.  
If step N fails, run **compensating transactions** for steps 1..N-1 (usually **reverse order**).

---

## 3. Two styles

### Orchestration (implemented)
- One **orchestrator** tells each service what to do
- Orchestrator stores saga state / timeline
- On failure → orchestrator triggers compensations

### Choreography
- No central boss
- Services publish/subscribe **events** (`OrderCreated`, `InventoryReserved`, `PaymentFailed`…)
- Each service decides its reaction + compensation

| Prefer orchestration when | Prefer choreography when |
|---|---|
| Complex flow, need visibility | Simple flow, strong event platform |
| Debugging/audit matters | Teams want loose coupling |

---

## 4. Our forward + compensate map

| # | Forward | Compensation |
|---|---|---|
| 1 | Create Order PENDING | Cancel Order |
| 2 | Reserve Inventory | Release Inventory |
| 3 | Charge Payment | Refund Payment |
| 4 | Confirm Order | (success end — no undo needed in happy path) |

**Rule:** Compensate **only completed** steps.

---

## 5. Failure scenarios (demo)

| Mode | What fails | Compensation |
|---|---|---|
| `NONE` | — | COMPLETED |
| `PAYMENT` | charge | Release inventory + Cancel order |
| `INVENTORY` | reserve | Cancel order only |

---

## 6. Real-life analogy

Booking a trip:
1. Reserve flight  
2. Reserve hotel  
3. Charge card  

If card fails → **cancel hotel**, **cancel flight** (compensations).  
You don’t leave a hotel reserved with no payment.

---

## 7. Saga vs 2PC vs Circuit Breaker

| | Saga | 2PC | Circuit Breaker |
|---|---|---|---|
| Goal | Business consistency across services | Atomic commit across resources | Protect caller from bad dependency |
| Consistency | Eventual | Strong (while it works) | N/A |
| Undo | Compensation | Rollback | Fallback / fail-fast |

They solve **different** problems; often used together (CB on each remote call inside a saga step).

---

## 8. Pitfalls (interview gold)

1. Compensations must be **idempotent** (retry-safe)  
2. Not every action has a perfect undo (email sent → send “cancel” email)  
3. Need **timeouts / saga state** for crash mid-flow  
4. **Isolation**: another customer might see reserved stock until compensate finishes  
5. Prefer **orchestration** for clear debugging in interviews unless asked about events

---

## 9. High-probability Q&A

**Q: Is Saga ACID?**  
A: No — it’s for **eventual consistency** (BASE-ish), not isolation like a single DB txn.

**Q: Orchestration vs Choreography?**  
A: Central coordinator vs event-driven reactions. Trade control/visibility vs coupling.

**Q: What if orchestrator crashes after inventory reserved?**  
A: Persist saga state; recovery job resumes or compensates. (Demo is in-memory — mention this limitation.)

**Q: Can we use @Transactional across services?**  
A: No — each service has its own DB. `@Transactional` is local only.

**Q: At compensation time, is data rolled back from DB?**  
A: No. **Interview line:** “Compensation is not database rollback — it’s a new committed action that reverses the business state.”

---

## 10. 90-second pitch

> "For cross-service order flow we use a Saga. My demo uses orchestration: create order, reserve inventory, charge payment. On payment failure we compensate in reverse — release stock and cancel the order — so we never keep inventory reserved without payment. That's eventual consistency without 2PC."
