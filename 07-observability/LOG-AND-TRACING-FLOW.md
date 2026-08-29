# Logs & Tracing Flow

**One checkout. One path. Two views: logs + trace.**

---

## Picture — one request across services

```text
  Customer taps "Pay"
           │
           │  requestId = REQ-42
           │  traceId   = T-99
           ▼
┌──────────────────┐
│   API Gateway    │  span: gateway (20ms)
│  set/forward ids │
└────────┬─────────┘
         │  headers travel with the call
         │  X-Request-Id: REQ-42
         │  traceparent:  ...T-99...
         ▼
┌──────────────────┐
│  Order Service   │  span: order (50ms)
│  Filter → MDC    │  log: "order O-7 created"
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Payment Service  │  span: payment (200ms) ← slow!
│  Filter → MDC    │  log: "card declined"
└──────────────────┘
```

---

## Same request — two ways you look at it

### A) Trace UI (path / timing)

```text
Trace T-99
├─ gateway     ████ 20ms
├─ order       ████████ 50ms
└─ payment     ████████████████████ 200ms  ← problem hop
```

**Answers:** Where did time go? Which service failed?

### B) OpenSearch logs (what each said)

```text
REQ-42 / T-99 | gateway | forwarding to order
REQ-42 / T-99 | order   | order O-7 created
REQ-42 / T-99 | payment | card declined
```

**Answers:** Why? Business message / error text.

→ Same ids glue **logs ↔ trace**.

---

## What is MDC? (easy picture)

**MDC = Mapped Diagnostic Context** — a small **bag of fields for the current request**.  
The logging framework copies those fields onto **every log line** on that thread. Controllers don’t pass ids around.

```text
  HTTP request enters Order Service
           │
           ▼
  ┌─────────────────────────────────────┐
  │  Filter (once)                      │
  │    MDC.put("requestId", "REQ-42")   │
  │    MDC.put("traceId",   "T-99")     │
  └─────────────────┬───────────────────┘
                    │  bag stays with this request
                    ▼
  OrderController / OrderService
       log.info("order O-7 created")
       log.warn("inventory low")
                    │
                    ▼
  Each log line automatically includes the bag:

       requestId=REQ-42  traceId=T-99  order O-7 created
       requestId=REQ-42  traceId=T-99  inventory low
```

| Without MDC | With MDC |
|---|---|
| You pass `requestId` into every method, or logs have no id | Filter sets once → all logs get ids |
| Easy to forget on one code path | One place (Filter) owns it |

**Remember:** MDC is usually **per thread**. Clear it when the request ends (`MDC.clear()`). On async/new threads, copy MDC or ids vanish.

---

## Who does what (short)

| Step | Who | Does |
|---|---|---|
| Enter Gateway | Filter | Create/reuse `REQ-42`, start/continue `T-99` |
| Inside a service | Filter + **MDC** | Put ids on every log — **no** `@RequestHeader` on every controller |
| Call next service | Client / OTel / Istio | Forward headers (`traceparent`, optional `X-Request-Id`) |
| Debug | You | Trace UI for path; OpenSearch for `REQ-42` or `T-99` |

---

## Rules of thumb

```text
✅ Filter + MDC once per service
✅ Propagate ids on outbound calls (library/mesh often does traceId)
❌ New random id at every service  → broken story
❌ Controllers must take requestId param  → usually unnecessary
```

---

## Our gateway demo vs production

| | Demo `:8080` | Production |
|---|---|---|
| Gateway sets `X-Request-Id` | ✅ | ✅ |
| Backend proves header arrived | ✅ (`@RequestHeader`) | Prefer **MDC** |
| Full trace waterfall | Notes | OTel / Istio + UI |
| OpenSearch | Your POC | Logs (+ traces if stored there) |

---

## 15-second pitch

> “One pay click gets a requestId and traceId at the gateway. Filters put them in MDC so every log line carries them; tracing headers ride each hop so the UI shows Gateway → Order → Payment. I use the trace to find the slow hop, and OpenSearch with the same ids to read why payment declined.”
