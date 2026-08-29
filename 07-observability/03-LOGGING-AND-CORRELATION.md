# 03 — Logging & Correlation

**Pillar:** **Logs** — “What happened in this service / for this request?”  
**Your POC home:** Elasticsearch → **OpenSearch** + OpenSearch Dashboards.

---

## 1. Why logs alone are not enough in microservices

One checkout may hit: Gateway → Order → Inventory → Payment.

If each service logs separately with **no shared id**, you cannot stitch:

```text
Gateway:  error calling order
Order:    reserved inventory
Payment:  card declined
```

into **one story**. You need a **correlation id** on every log line for that request.

---

## 2. Correlation ids (must know)

| Id | Meaning | Typical header |
|---|---|---|
| **requestId** / **X-Request-Id** | Correlate logs for **this** HTTP call (you used this at API Gateway) | `X-Request-Id` |
| **traceId** | Correlate the **whole distributed trace** (all spans of one request) | W3C `traceparent` / B3 |
| **spanId** | One hop/operation inside a trace | part of tracing |

**Interview line:**  
> “Request id is for log correlation at the edge; trace id ties logs to distributed tracing across services. In good setups both appear in MDC/log fields.”

You already practiced **X-Request-Id** in the API Gateway pattern — same idea here.

---

## 3. Good logging habits

| Do | Don’t |
|---|---|
| Structured logs (JSON) with `requestId`, `traceId`, `service`, `level` | Only free-text with no ids |
| Log business outcome + error cause | Log secrets, full cards, passwords |
| Same id propagated to downstream (header) | New random id at every service |
| Levels: ERROR/WARN for ops, DEBUG locally | INFO spam that hides failures |

**Propagation:** Gateway generates or accepts `X-Request-Id` → pass to Order → Payment so every log share that field.

---

## 4. Pipeline (how logs leave the app)

```text
Spring Boot app
   │  logback / log4j2  (JSON + MDC: requestId, traceId)
   ▼
Node agent / sidecar collector
   (Fluent Bit, Fluentd, Filebeat, OTel Collector, …)
   ▼
OpenSearch (was Elasticsearch in older stacks)
   ▼
OpenSearch Dashboards
   search: requestId:"abc-123"  OR  traceId:"..."
```

| Layer | Role |
|---|---|
| **App** | Emit logs + put ids in MDC |
| **Shipper** | Tail/forward logs from pods |
| **OpenSearch** | Index + search store |
| **Dashboards** | UI to query/alert |

---

## 5. Your project: Elasticsearch → OpenSearch

| Topic | What to say |
|---|---|
| **What changed** | Log **backend** migrated ES → OpenSearch (API-compatible family; licensing/ops reasons often drive this) |
| **What stayed** | Log format, shippers, dashboards habit — still search by correlation id |
| **Why it matters** | Central search across services; incident: filter by `requestId`/`traceId` and see full path in logs |

**Not the same as:**
- Actuator **health** (concept 02)  
- **Metrics** Prometheus (concept 04)  
- Full **trace waterfall** UI (concept 05) — though OpenSearch may also show traces if you store them there  

Logs on OpenSearch Dashboards = **this section (03)**.  
Traces on OpenSearch Dashboards = mainly **05**, linked by the same `traceId`.

---

## 6. How logs connect to traces

```text
Same request
   │
   ├─► Logs in OpenSearch     fields: traceId=T1, requestId=R1
   └─► Trace UI (Zipkin/Jaeger/OS)   traceId=T1
```

If MDC includes `traceId`, you jump from a log line to the trace (or the reverse).

---

## 7. Spring-ish sketch (conceptual)

```text
Filter / Gateway
  ensure X-Request-Id
  MDC.put("requestId", id)
  → call downstream with same header

log.info("order created orderId={}", orderId);
 // JSON: {"requestId":"...","traceId":"...","message":"order created ..."}
```

Libraries often used: Logback JSON encoder, Micrometer Tracing / OTel bridge for `traceId` in MDC.

**Deep dive (visual flow):** → **`LOG-AND-TRACING-FLOW.md`**

---

## 8. Pitch (20 seconds)

> “We use structured logs with request and trace ids propagated from the gateway. Logs are shipped to OpenSearch — we migrated from Elasticsearch — and we search incidents in OpenSearch Dashboards by correlation id. That answers what each service did; metrics and traces answer how bad and where across the mesh.”

---

## Checkpoint

1. Why correlation ids in microservices?  
2. requestId vs traceId?  
3. Where does ES→OpenSearch sit? (**log storage/search**, concept 03)  
4. How do logs link to tracing? (shared **traceId**)

When ready → concept **4: Metrics**.
