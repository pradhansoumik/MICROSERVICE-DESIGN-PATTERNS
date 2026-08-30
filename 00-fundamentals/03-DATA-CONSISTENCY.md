# 03 — Data Consistency (Short)

**Common question:** “How do you keep data consistent across services?”

---

## Core idea

Each service owns its **DB** → no single ACID transaction across Order + Inventory + Payment.  
Use **Saga** (local TX + compensations) and/or **events** → **eventual consistency**.

| Approach | When |
|---|---|
| Local `@Transactional` | Inside one service |
| **Saga orchestration / choreography** | Business workflow across services |
| Avoid **2PC** across microservices | Fragile, locking, poor fit for cloud |

**Demos / notes:** `02-saga/` · `02-saga/CHOREOGRAPHY.md` · `02-saga/2PC-ORCHESTRATION-CHOREOGRAPHY.md`

**Pitch:**  
> “We don’t use distributed 2PC. We use sagas: each step commits locally; on failure we compensate. Reads that need a join often use CQRS read models or API composition.”
