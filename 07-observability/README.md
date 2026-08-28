# 07 — Observability (learn one by one)

**Status:** Concepts first → small Spring demos later  
**Why:** In microservices, “the API failed” is not enough — you need **where**, **why**, and **how bad**.

**Your OCP POC map:** → **`REAL-PROJECT-MAPPING.md`** (OpenSearch logs/traces, Istio/Kiali)

---

## Learn path (this order)

| # | Concept | Note | Status |
|---|---|---|---|
| 1 | What is Observability (+ 3 pillars) | `01-WHAT-IS-OBSERVABILITY.md` | ✅ Done |
| 2 | Health & Actuator | `02-HEALTH-AND-ACTUATOR.md` | **Read next** |
| 3 | Logging & correlation (`requestId` / `traceId`) | `03-LOGGING-AND-CORRELATION.md` | Pending — **your ES→OpenSearch POC** |
| 4 | Metrics (Micrometer / Prometheus / Grafana) | `04-METRICS.md` | Pending — + Istio/Kiali metrics mention |
| 5 | Distributed Tracing (spans, Zipkin/OTel) | `05-DISTRIBUTED-TRACING.md` | Pending — **OpenSearch trace UI + Istio** |
| 6 | Putting it together + interview pitch | `06-PITCH-AND-CHECKLIST.md` | Pending — full POC story |

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

Open **`02-HEALTH-AND-ACTUATOR.md`**.  
Your OpenSearch / Istio details land mainly in **03** and **05** (see `REAL-PROJECT-MAPPING.md`).
