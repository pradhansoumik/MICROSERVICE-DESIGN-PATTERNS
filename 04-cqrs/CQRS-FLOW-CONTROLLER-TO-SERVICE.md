# CQRS — Flow: Controller → Service → Event → Projector

Demo: `04-cqrs/cqrs-demo` on port **8091**

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
Right after the write model is saved successfully, still inside `OrderCommandService.placeOrder()` / `cancelOrder()`.

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
done (no response to client from projector)
```

**When is the projector invoked?**  
Automatically when `OrderChangedEvent` is published — after write save, before/around the HTTP response returning (in-process, sync by default in Spring).

Same for **cancel**:
`cancelOrder` → update WRITE → publish event → projector updates READ to `CANCELLED`.

---

## C) Query path — no event / no projector

```text
Client
  │  GET /api/queries/orders/{id}
  ▼
CqrsController.getOrder()
  │  queryService.getById(id)
  ▼
OrderQueryService
  │  readRepository.findById(...)   ← READ DB only
  ▼
returns OrderReadModel
```

Query side **never** writes and **never** publishes events.

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
| HTTP commands / queries | `api/CqrsController` |
| Write logic | `command/OrderCommandService` |
| Write store | `command/OrderWriteRepository` |
| Event | `shared/OrderChangedEvent` |
| Projector | `projection/OrderReadModelProjector` |
| Read logic | `query/OrderQueryService` |
| Read store | `query/OrderReadRepository` |
