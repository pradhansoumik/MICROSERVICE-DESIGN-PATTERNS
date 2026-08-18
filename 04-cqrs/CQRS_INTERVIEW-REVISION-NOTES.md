# CQRS — Interview Revision Notes

**Demo:** `04-cqrs/cqrs-demo` on **:8091**  
**Category:** Data / Database pattern

---

## 1. What is CQRS?

**Command Query Responsibility Segregation**  
- **Command** = change state (write)  
- **Query** = return data (read)  
- Different models/paths for each

---

## 2. Why use it?

| Pain | CQRS help |
|---|---|
| One fat domain model for UI + writes | Separate write rules vs read shapes |
| Heavy read load | Scale read DB / cache independently |
| Complex reporting joins | Precomputed / denormalized read model |
| Different consistency needs | Writes strict; reads can be eventual |

---

## 3. Our demo mapping

| Side | API | Storage |
|---|---|---|
| Command | `POST /api/commands/orders` | `OrderWriteModel` |
| Command | `POST /api/commands/orders/{id}/cancel` | update write model |
| Query | `GET /api/queries/orders/{id}` | `OrderReadModel` |
| Query | `GET /api/queries/orders?customerId=` | list read models |
| Sync | `OrderChangedEvent` → projector | write → read |

---

## 4. Real-life analogy

**Library**
- **Write:** librarian updates master catalog carefully (commands)  
- **Read:** public search kiosk shows a simplified card view (queries)  
- Overnight sync updates kiosk from catalog (projection)

---

## 5. CQRS vs related patterns

| Pattern | Relation |
|---|---|
| **Database per Service** | CQRS can use 2 DBs even inside one service |
| **Saga** | Saga = cross-service consistency; CQRS = read/write split |
| **Event Sourcing** | Often paired with CQRS (events are write store); CQRS ≠ must use ES |
| **API Gateway** | Gateway routes; CQRS is behind services |

---

## 6. Consistency

- Write commit is source of truth for changes  
- Read model may lag → **eventual consistency**  
- Demo uses in-process events (fast); prod often Kafka → seconds of lag is OK for lists

---

## 7. When NOT to use CQRS

- Simple CRUD apps  
- Small team / no read scaling pain  
- Adds complexity (two models, sync, troubleshooting)

**Interview honesty:** CQRS is powerful but easy to over-engineer.

---

## 8. Q&A

**Q: Is CQRS a database pattern?**  
A: Yes — data/read-write modeling pattern in MS interviews.

**Q: Do we need two microservices?**  
A: No. CQRS can be two models inside one service (our demo) or separate services/DBs.

**Q: CQRS vs Event Sourcing?**  
A: CQRS = separate read/write. Event Sourcing = store events as truth. Often combined, not the same.

**Q: Strong consistency?**  
A: Writes can be strongly consistent locally; read side is often eventually consistent with write side.

---

## 9. 90-second pitch

> “CQRS splits commands and queries. In my order demo, place/cancel hit a write model; list/detail APIs hit a denormalized read model updated by a projector. Demo uses in-process Spring events; in production I’d use Kafka/CDC for async projection so reads scale independently with eventual consistency.”
