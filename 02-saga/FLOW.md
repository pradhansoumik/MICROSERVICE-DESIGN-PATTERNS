# Saga — FLOW (Orchestration + Compensation)

Demo: `saga-demo` on port **8090**

---

## Happy path (COMPLETED)

```text
Client
  │  POST /api/saga/orders
  ▼
SagaController.placeOrder()
  │  orchestrator.placeOrder(request)
  ▼
OrderSagaOrchestrator
  │  1) orderService.createOrder()       → ORDER_CREATED
  │  2) inventoryService.reserve()       → INVENTORY_RESERVED
  │  3) paymentService.charge()          → PAYMENT_COMPLETED
  │  4) orderService.confirmOrder()      → COMPLETED
  ▼
SagaController → SagaResponse (COMPLETED + timeline)
```

---

## Compensation path (e.g. mode=PAYMENT)

```text
Client → SagaController → OrderSagaOrchestrator
  │
  ├─① createOrder()     ✅  orderId saved
  ├─② reserve()         ✅  reservationId saved
  ├─③ charge()          ❌  throws
  │
  │  catch → status COMPENSATING → compensate(saga)
  │
  ├─④ compensate() [reverse, only completed steps]
  │     paymentId == null     → skip refund
  │     reservationId set     → inventoryService.release()
  │     orderId set           → orderService.cancelOrder()
  │     status = COMPENSATED
  ▼
SagaResponse (COMPENSATED + timeline)
```

---

## Who calls whom

| Layer | Class | Role |
|---|---|---|
| API | `SagaController` | HTTP; sets failure-mode |
| Orchestrator | `OrderSagaOrchestrator` | Forward steps + compensate |
| Local services | `OrderService`, `InventoryService`, `PaymentService` | Local commit + compensate methods |

Services **do not** call each other — only the orchestrator does.

---

## Important

- Compensation ≠ DB `ROLLBACK` — new UPDATE/INSERT commits that reverse business state  
- Choreography (event-driven saga) — **not in this demo**; planned with Event-Driven later  

---

## Interview line

> “Controller calls the saga orchestrator, which runs local steps in order. On payment failure it compensates in reverse — release inventory, cancel order — with new commits, not a global DB rollback. Production would persist saga state for crash recovery; choreography would replace the orchestrator with domain events.”

Related notes: `SAGA_INTERVIEW-REVISION-NOTES.md`, `2PC-ORCHESTRATION-CHOREOGRAPHY.md`, `DEMO.md`
