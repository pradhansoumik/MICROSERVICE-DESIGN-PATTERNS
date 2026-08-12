# 2PC vs Orchestration vs Choreography (Saga)

Interview-oriented note: why Saga, how it differs from 2PC, orchestration vs choreography, and transactions while compensating.

E-commerce flow: Order → Inventory → Payment.

---

## 1. Why we chose Saga

### The real problem
In microservices, each service owns its **own database** (Database per Service):

| Service | Own data |
|---|---|
| Order | orders table |
| Inventory | stock / reservations |
| Payment | payments / charges |

A single place-order business action needs **all three** to stay consistent:
- Don’t confirm an order if payment failed  
- Don’t keep stock reserved if order is cancelled  
- Don’t charge if inventory couldn’t be reserved  

### Why not a normal `@Transactional`?
`@Transactional` / local ACID works **inside one service / one DB only**.  
It **cannot** span Order DB + Inventory DB + Payment DB.

### Why not 2PC (two-phase commit)?
| 2PC | Reality in microservices |
|---|---|
| Strong atomic commit across resources | Slow, locks held longer |
| Coordinator + prepare/commit | Poor fit with polyglot DBs, cloud, partial outages |
| Blocking | One slow participant blocks everyone |

Modern MS teams almost never use classic 2PC for business workflows across services.

#### Is 2PC only for shared DB?
**No.** In fact a **single shared DB** usually does **not** need 2PC — one database already gives you local ACID (`BEGIN` / `COMMIT` / `ROLLBACK`).

**2PC is for multiple resource managers** that must commit or abort **together**, for example:
- DB-A + DB-B  
- DB + message broker (JMS)  
- DB + another XA-capable system  

```text
Shared one DB     → normal local transaction (not 2PC)
Two DBs / DB+Queue → 2PC/XA possible (if both support it)
Microservices      → usually Saga instead of 2PC
```

#### Real-life example where 2PC/XA was used
**Classic banking / enterprise monolith (same company, XA resources):**

> A payment module must:  
> 1) Debit customer account in **Oracle DB**  
> 2) Write an audit/payment event to **IBM MQ / JMS queue**  
>  
> Both must succeed or both must fail (no “money deducted but event never published”).  
> App server (WebLogic/WebSphere) runs an **XA 2PC** transaction across Oracle + MQ.

Another example: **old dual-database** setup — update **orders DB** and **inventory DB** in one XA transaction when both DBs are XA-capable and co-located under one transaction manager.

**Why we still don’t use it for our microservice Order→Inventory→Payment:**
- Separate deployable services, often different tech stacks  
- Network partitions / long locks hurt availability  
- Saga + compensations scale better operationally  

**Interview one-liner:**  
> “2PC is not ‘shared DB’ — it’s atomic commit across multiple resources (e.g. DB + queue). Shared single DB uses normal local txns. Cross-microservice flows prefer Saga over 2PC.”


### Why Saga fits
Saga = **sequence of local transactions** + **compensating actions** if a later step fails.

| Without Saga | With Saga |
|---|---|
| Payment fails but inventory stays reserved | Payment fails → **release inventory** + **cancel order** |
| Try to fake one big DB txn | Accept **eventual consistency** with explicit undo |

### One-liner (why Saga)
> “We chose Saga because cross-service order flow needs consistency without a shared DB or 2PC — local commits plus compensations give us eventual consistency.”

---

## 2. Orchestration vs Choreography — which is better?

**There is no universal winner.** Choose based on flow complexity, team ownership, and ops needs.

### Quick comparison

| | **Orchestration** (our demo) | **Choreography** (later) |
|---|---|---|
| Who drives the flow? | Central **orchestrator** | Each service reacts to **events** |
| Visibility | High — one place to read the saga | Lower — flow spread across topics/consumers |
| Coupling | Orchestrator knows all steps | Services coupled via event contracts |
| Change of flow | Change orchestrator | May touch multiple event handlers |
| Debugging | Easier (timeline in one service) | Harder (distributed tracing needed) |
| Single point of failure risk | Orchestrator must be reliable + durable state | No single boss; broker + consumers must be solid |
| Typical tools | Custom orchestrator, Temporal, Camunda, Conductor | Kafka/Rabbit + domain events |

### When orchestration is better
- Complex multi-step flows (order, travel booking, loan approval)
- Strong need for **audit / saga state / support tooling**
- You want one team to own the **business workflow**
- Interview demos / first implementation (clearer to explain)

→ **We chose orchestration for this demo** for clarity and control.

### When choreography is better
- Simple, stable flows (2–3 steps)
- Teams already event-driven (Kafka mature)
- You want services to stay unaware of the full workflow
- High autonomy between bounded contexts

### Practical recommendation (what to say in interviews)
> “For complex order workflows I prefer **orchestration** for visibility and compensation control. For simple event-native domains I’d use **choreography**. Many products mix both: choreography for notifications, orchestration for money/stock critical paths.”

**Better approach for our Order→Inventory→Payment case:**  
**Orchestration** is usually the better default (money + stock = need clear compensation ownership).

---

## 3. How Saga manages “transactions” in real time while compensating

Important: Saga does **not** hold one big open transaction across services.

### What “transaction” means here
Each step is its **own local transaction** (committed immediately):

```text
1) Order DB:    INSERT order PENDING          → COMMIT (local)
2) Inventory DB: RESERVE stock                → COMMIT (local)
3) Payment DB:  CHARGE card                   → COMMIT (local)  OR FAIL
4) Order DB:    UPDATE order CONFIRMED        → COMMIT (local)
```

There is **no** global lock waiting for all three.

### Real-time happy path
```text
t1  Order created      (committed)
t2  Inventory reserved (committed)  ← other users may already “see” less stock
t3  Payment captured   (committed)
t4  Order confirmed    (committed)
    → Saga COMPLETED
```

Business is consistent at the end; mid-flight you may have temporary states (PENDING order, RESERVED stock).

### Real-time compensation path (payment fails)
```text
t1  Order created      COMMIT ✅
t2  Inventory reserved COMMIT ✅
t3  Payment charge     FAIL  ❌
    ── orchestrator starts compensation (still “real time” in the request, or async worker) ──
t4  Inventory release  COMMIT ✅  (undo step 2)
t5  Order cancel       COMMIT ✅  (undo step 1)
    → Saga COMPENSATED
```

**Rules while compensating:**
1. **Only undo steps that already committed** (if no `paymentId`, skip refund).  
2. Compensate in **reverse order** (release inventory before relying on order cancel semantics as designed).  
3. Each compensation is again a **local commit** (not a rollback of an old global txn — that old local txn is already done).  
4. Compensations should be **idempotent** (safe if retried after crash).  

### “Rollback” vs “compensation” (say this clearly)
| Classic DB rollback | Saga compensation |
|---|---|
| Undo **uncommitted** work in one txn | Issue a **new** business action that reverses **already committed** work |
| `ROLLBACK;` | `releaseReservation()`, `refund()`, `cancelOrder()` |

So in real time we are not “rolling back payment+inventory together”; we are **forward-fixing the mistake** with compensating local transactions.

#### Production FAQ: At compensation time, is data “rolled back” from DB?
**No — not a classic `ROLLBACK` of the original transaction.**

That original local transaction already **committed**. The DB cannot “un-commit” it later with `ROLLBACK`.

What production systems do instead:

| After forward step | During compensation (new txn) |
|---|---|
| `orders.status = PENDING` (committed) | `UPDATE orders SET status='CANCELLED'` (new commit) |
| Stock row reserved (committed) | `UPDATE` / delete reservation / add stock back (new commit) |
| Payment captured (committed) | Insert **refund** payment / call PSP refund API (new commit) |

So data is **changed again** to reverse business effect — not rolled back like an open SQL transaction.

Sometimes people say “rollback the business state” loosely in conversation; technically it is **compensation**, not DB rollback.

**Interview line:**  
> “Compensation is not database rollback — it’s a new committed action that reverses the business state.”

**Simple mental model:**  
For the **same order**, compensation is usually either:
- another **`UPDATE` + `COMMIT`** on the same row (e.g. `PENDING` → `CANCELLED`), and/or  
- another **new record** + `COMMIT` (e.g. insert a `REFUND` row linked to the original payment)

It is **not** undoing the old transaction log — it is a **new** alter/insert that reverses the business effect.

### What about crash mid-compensation? (production reality)
In-memory demo is simplified. In real systems:

| Concern | Approach |
|---|---|
| Orchestrator crash after reserve, before compensate | Persist **saga state** (step status); recovery job resumes/compensates |
| Duplicate compensate calls | Idempotent APIs + idempotency keys |
| Long-running steps | Async saga + timeouts + alerts |
| Visibility to users | Order shows `PENDING` / `CANCELLED`; stock freed after compensate |

### Consistency model
- **Not** strong ACID across services  
- **Eventual consistency**: after compensation finishes, system is correct again  
- Window of inconsistency is expected and must be designed for (PENDING states, reservations with TTL, etc.)

### One-liner (transactions + compensate)
> “Each saga step commits locally in real time. On failure we don’t globally roll back — we run new compensating transactions to undo completed steps until the business state is consistent again.”

---

## 4. End-to-end picture ( Ord + Inv + Pay )

```text
Why Saga?
  Database per service → no shared @Transactional → avoid 2PC → use Saga

Which style?
  Critical money/stock flow → Orchestration (our choice)
  Simple event mesh       → Choreography (later)

How txn works?
  Forward:  local COMMIT per step
  Failure:  compensate with new local COMMITs (reverse, completed steps only)
  Result:   eventual consistency, not one big ACID txn
```

---

## 5. Short interview answers (copy-ready)

**Q: Why Saga?**  
Because services don’t share a DB; we need cross-service consistency without 2PC.

**Q: Orchestration or choreography — which is better?**  
Depends. For complex/critical flows (orders, payments), orchestration is often better for control and debugging. Choreography fits simple event-driven flows. We used orchestration for the order saga.

**Q: How do you manage transactions while compensating?**  
Steps already committed locally aren’t rolled back globally. The orchestrator issues compensating local transactions (refund/release/cancel), ideally idempotent and reverse-ordered, until state is consistent.

**Q: At compensation time, is data rolled back from DB?**  
No. **Interview line:** “Compensation is not database rollback — it’s a new committed action that reverses the business state.”
