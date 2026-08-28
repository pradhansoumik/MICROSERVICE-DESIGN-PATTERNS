# Your real project POC → which Observability section

Use this when studying concepts **and** in interviews (“what did you do in your project?”).

---

## Quick map

| Your POC | Pillar / topic | Exact section in this folder | When you learn it |
|---|---|---|---|
| **Elasticsearch → OpenSearch** (log store/migrate) | **Logs** (storage / search backend) | **`03-LOGGING-AND-CORRELATION.md`** | Concept 3 |
| **Logs visible on OpenSearch Dashboards** | **Logs** (UI / search) | **`03`** (+ mention again in **`06`** pitch) | Concept 3 |
| **Tracing on OpenSearch dashboard** | **Traces** (UI / backend may be OpenSearch or OTel→OS) | **`05-DISTRIBUTED-TRACING.md`** | Concept 5 |
| **Istio + Kiali** | **Service mesh observability** (topo, traffic, some metrics/traces) | **`05`** (mesh + tracing) + short note in **`04-METRICS.md`** + **`06`** | Concepts 4–6 |
| Probes / health on OCP | **Health** | **`02-HEALTH-AND-ACTUATOR.md`** | Concept 2 (now) |

---

## How to talk about each

### 1) Elasticsearch → OpenSearch (logging)
- **Section:** Concept **3 — Logging**
- **Story:** Apps emit logs → shipped (Fluent Bit / Fluentd / Logstash / collector) → **OpenSearch** index → search/alert in **OpenSearch Dashboards**
- **Not:** replacing metrics or Actuator health
- **Interview:** “We migrated the log backend from Elasticsearch to OpenSearch; correlation ids in logs still key for request search.”

### 2) Tracing on OpenSearch dashboard
- **Section:** Concept **5 — Distributed Tracing**
- **Story:** Each request creates **spans** (OpenTelemetry / Jaeger / Zipkin-compatible) → stored or queried via OpenSearch (or OTel collector → OpenSearch) → **trace UI** on dashboard
- **Link to gateway:** `traceId` / `requestId` from earlier API Gateway notes
- **Interview:** “Traces show the hop-by-hop path; we view them on OpenSearch Dashboards alongside related logs when ids match.”

### 3) Istio + Kiali
- **Sections:** mainly **5 (tracing/mesh)**, also **4 (metrics)** and **6 (full picture)**
- **Story:** Sidecar (Envoy) gets traffic metrics / can participate in tracing → **Kiali** shows service graph, success rates, maybe golden metrics
- **This is platform/mesh observability**, not Spring Actuator itself
- **Interview:** “On OCP we use Istio; Kiali gives a live service-mesh topology and traffic health. App-level logs still go to OpenSearch; Actuator still used for probes.”

---

## Where they sit on the 3 pillars

```text
LOGS                          METRICS                     TRACES
────────                      ───────                     ──────
OpenSearch (from ES)          Prometheus/Grafana          Trace UI on OpenSearch
OpenSearch Dashboards         (+ Istio/Kiali traffic)     (+ Istio distributed tracing)
correlation in log fields     Micrometer/Actuator         traceId across services
```

Kiali overlaps **metrics + traces + topology**; put the **deep** explanation under **tracing/mesh (05)**, and one line under **metrics (04)**.

---

## Suggested study order (unchanged) + your POC

| Concept | Doc | Bring in your POC? |
|---|---|---|
| 1 What / pillars | `01` | High-level: “we use OS for logs/traces, Istio/Kiali for mesh” |
| 2 Health | `02` | OCP probes (separate from OpenSearch/Kiali) |
| 3 Logging | `03` | **ES → OpenSearch + dashboards** ← main home |
| 4 Metrics | `04` | Micrometer/Prometheus; **one paragraph on Istio/Kiali metrics** |
| 5 Tracing | `05` | **Tracing on OpenSearch + Istio** ← main home for Kiali/traces |
| 6 Pitch | `06` | Combined production story |

---

## One interview paragraph (copy/adapt)

> “For observability we use the three pillars. Logs were migrated from Elasticsearch to OpenSearch and we search them in OpenSearch Dashboards with correlation ids. Distributed traces are also available on the OpenSearch dashboard so we can follow a request across services. On OpenShift, Istio with Kiali gives service-mesh topology and traffic health. Pod health still uses Actuator liveness/readiness probes.”
