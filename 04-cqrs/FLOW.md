# CQRS — FLOW

Demo: `cqrs-demo` on port **8091**

Standard flow doc for this pattern (same content as the detailed controller→service note).

---

## A) Command path — place order

```text
Client
  │  POST /api/commands/orders
  ▼
CqrsController.placeOrder()
  │  commandService.placeOrder(command)
  ▼
OrderCommandService
  │  1) build OrderWriteModel (CREATED)
  │  2) writeRepository.save(...)     ← WRITE DB
  │  3) events.publishEvent(OrderChangedEvent)   ← EVENT FIRED HERE
  ▼
returns orderId to Controller → Client
```

**When is the event invoked?**  
Right after the write model is saved successfully, inside `OrderCommandService.placeOrder()` / `cancelOrder()`.

```java
writeRepository.save(order);
events.publishEvent(new OrderChangedEvent(...));  // ← here
```

---

## B) Projector — sync write → read

```text
ApplicationEventPublisher
  │  Spring delivers OrderChangedEvent
  ▼
OrderReadModelProjector.on(event)   ← @EventListener
  │  build OrderReadModel (denormalized + displaySummary)
  │  readRepository.save(view)      ← READ DB
  ▼
done (no HTTP response from projector)
```

**When is the projector invoked?**  
Automatically when `OrderChangedEvent` is published — after write save (in-process; Spring default is synchronous).

Cancel path: `cancelOrder` → update WRITE → publish event → projector sets READ to `CANCELLED`.

---

## C) Query path — no event / no projector

```text
Client
  │  GET /api/queries/orders/{id}
  ▼
CqrsController → OrderQueryService → readRepository.findById
  ▼
OrderReadModel
```

Query side never writes and never publishes events.

---

## End-to-end picture

```text
COMMAND:
  Controller → CommandService → WriteRepo.save
                              → publish OrderChangedEvent
                                         ↓
                               Projector (@EventListener)
                                         ↓
                               ReadRepo.save (projection)

QUERY:
  Controller → QueryService → ReadRepo.find   (no event)
```

---

## Interview line

> “Commands update the write model and publish an event; the projector updates the read model. Queries only read the read model — they don’t touch the write path. In this demo we use Spring’s in-process `ApplicationEventPublisher` for simplicity; in a real production project I’d publish to Kafka (or RabbitMQ) — or use CDC — and run an async projector/consumer so write and read models scale independently with eventual consistency.”

---

## Code map

| Step | Class |
|---|---|
| HTTP | `api/CqrsController` |
| Write | `command/OrderCommandService` |
| Write store | `command/OrderWriteRepository` |
| Event | `shared/OrderChangedEvent` |
| Projector | `projection/OrderReadModelProjector` |
| Read | `query/OrderQueryService` |
| Read store | `query/OrderReadRepository` |

Related: `CQRS-FLOW-CONTROLLER-TO-SERVICE.md` (same flow, longer title), `DEMO.md`
