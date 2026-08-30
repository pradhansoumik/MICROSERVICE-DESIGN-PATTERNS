# 07 — Observability (learn one by one)

**Status:** Concepts first → small Spring demos later  
**Why:** In microservices, “the API failed” is not enough — you need **where**, **why**, and **how bad**.

**Your OCP POC map:** → **`REAL-PROJECT-MAPPING.md`** (OpenSearch logs/traces, Istio/Kiali)

---

## Learn path (this order)

| # | Concept | Note | Status |
|---|---|---|---|
| 1 | What is Observability (+ 3 pillars) | `01-WHAT-IS-OBSERVABILITY.md` | ✅ Done |
| 2 | Health & Actuator | `02-HEALTH-AND-ACTUATOR.md` | ✅ Done (or in progress) |
| 3 | Logging & correlation (`requestId` / `traceId`) | `03-LOGGING-AND-CORRELATION.md` | ✅ |
| — | Logs & tracing visual flow | `LOG-AND-TRACING-FLOW.md` | Companion to 03 |
| 4 | Metrics (Micrometer / Prometheus / Grafana) | `04-METRICS.md` | ✅ |
| 5 | Distributed Tracing (spans, OTel / OpenSearch / Istio) | `05-DISTRIBUTED-TRACING.md` | ✅ |
| 6 | Putting it together + interview pitch | `06-PITCH-AND-CHECKLIST.md` | **Read next** |

Runnable demo app(s) come after concepts (or after each pillar if you want hands-on sooner).

---

## Already touched in earlier patterns

| Idea | Where you saw it |
|---|---|
| `X-Request-Id` | API Gateway filters |
| Trace vs request id | Gateway interview notes |
| Actuator health / CB metrics | Circuit Breaker demos |

Observability ties those into one story: **logs + metrics + traces**.

---

## Next

Open **`06-PITCH-AND-CHECKLIST.md`** — series wrap-up.  
After that: optional small metrics/tracing demo, or **Event-Driven** when Kafka is done.
