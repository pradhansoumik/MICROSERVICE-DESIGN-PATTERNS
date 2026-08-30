# 05 — Distributed Tracing

**Pillar:** **Traces** — “Where did **this one request** go, and which hop was slow/failed?”  
**Your POC:** traces on **OpenSearch Dashboards** + **Istio** (often with **Kiali**).

Related visual: **`LOG-AND-TRACING-FLOW.md`**

---

## 1. What a trace is

```text
One Pay click = ONE trace (one traceId)
├─ span: API Gateway
├─ span: Order Service
└─ span: Payment Service   ← e.g. slow or error
```

| Term | Meaning |
|---|---|
| **traceId** | Id for the **whole** journey |
| **spanId** | Id for **one** operation/hop |
| **span** | Timed unit of work (name, start/end, tags, error) |

**Not the same:** `traceId` ≠ `spanId` (trip vs checkpoint).

---

## 2. Why traces (vs logs / metrics)

| Tool | Answers |
|---|---|
| **Metrics** | “Payment p99 is up” (fleet) |
| **Logs** | “card declined” (detail) |
| **Traces** | “This request spent 200ms in Payment after Order” (path) |

Incident habit: **alert (metrics) → pick a slow trace → logs with same traceId**.

---

## 3. How it works (short)

```text
1) First service creates/continues trace context
2) Outbound call sends header (W3C traceparent — or B3)
3) Next service creates a child span under same traceId
4) Spans exported → collector / backend → UI
```

Propagation is required; **libraries or Istio** usually do the header work — not every controller method.

**Spring today:** Micrometer Tracing + OpenTelemetry (Brave/Zipkin or OTel exporter).  
Older texts: Spring Cloud Sleuth (now evolved into Micrometer Tracing).

---

## 4. Backends / UIs you’ll hear

| Stack | Role |
|---|---|
| **Zipkin / Jaeger** | Classic trace UIs |
| **OpenTelemetry Collector** | Receives spans, exports to many backends |
| **OpenSearch** (+ Dashboards) | **Your POC** — store/search/view traces |
| **Istio + Kiali** | Mesh can generate/propagate traffic traces + show graph |

App can export OTLP → collector → OpenSearch (or vendor APM). Exact pipeline varies; the **idea** is the same: spans with one `traceId`.

---

## 5. Sampling (interview awareness)

Not every request is always stored (cost/volume).

| Idea | Meaning |
|---|---|
| **Head-based sampling** | Decide at start: keep or drop this trace |
| Keep errors / slow ones | Common production policy |

Demo/dev: often sample 100%. Prod: often lower %.

---

## 6. Istio / Kiali vs app instrumentation

| Layer | What you get |
|---|---|
| **App OTel / Micrometer** | Business spans, DB/HTTP client detail inside the service |
| **Istio** | Hop-level traffic spans between services; header propagation help |
| **Kiali** | Topology + traffic health; can link into tracing |

Best: mesh **and** app instrumentation. Mesh alone may miss deep in-process spans.

---

## 7. Link to logs (you already learned)

```text
MDC: traceId=T-99
OpenSearch logs: filter traceId=T-99
Trace UI: open T-99   ← same id
```

---

## 8. Pitch (20 seconds)

> “Distributed tracing gives one traceId per request and a span per hop so we see Gateway → Order → Payment timing. Context propagates via traceparent, often with OpenTelemetry or Istio. We view traces on OpenSearch Dashboards; Kiali helps with mesh topology. Metrics alert us; the trace shows where; logs with the same traceId explain why.”

---

## Checkpoint

1. traceId vs spanId?  
2. What header carries context (modern default)?  
3. Metrics → traces → logs: order in an incident?  
4. OpenSearch vs Istio/Kiali for tracing?

### Answers

1. **traceId** = whole request; **spanId** = one hop/operation.  
2. **W3C `traceparent`** (B3 also exists in older setups).  
3. **Metrics** alert → **trace** find hop → **logs** (same traceId) for detail.  
4. **OpenSearch** = store/UI for traces (your POC). **Istio/Kiali** = mesh-level telemetry/graph (+ tracing integration); not a full replacement for app spans.

When ready → concept **6: Pitch & checklist**.
