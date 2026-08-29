# 04 — Metrics

**Pillar:** **Metrics** — “How is the system doing over time?”  
**Spring stack:** Micrometer → Actuator → Prometheus → Grafana  
**Mesh angle:** Istio / Kiali also expose traffic metrics.

---

## 1. What metrics answer

| Question | Example metric |
|---|---|
| How many errors? | `http_server_requests_seconds_count` with status 5xx |
| How slow? | p95 / p99 latency |
| How busy? | requests/sec, JVM heap, CPU |
| Is Payment unhealthy? | custom `payment_failures_total` or CB state gauges |

Unlike **one** log line or **one** trace, metrics are **aggregated over time** → perfect for **alerts** and dashboards.

---

## 2. Easy picture

```text
  Many requests over 5 minutes
           │
           ▼
  Micrometer records counts / timers
           │
           ▼
  GET /actuator/prometheus   ← Prometheus scrapes
           │
           ▼
  Grafana dashboard + alerts
  "Payment p99 > 1s" / "5xx rate > 2%"
```

**Analogy:** Logs = diary entries. Trace = one parcel’s GPS. **Metrics = speedometer + odometer** for the fleet.

---

## 3. Metric types (say these in interviews)

| Type | Meaning | Example |
|---|---|---|
| **Counter** | Only goes up | `orders_created_total` |
| **Gauge** | Up/down current value | JVM memory used, CB state |
| **Timer** / histogram | Duration + count | HTTP request latency |

---

## 4. Spring Boot wiring (conceptual)

```text
spring-boot-starter-actuator
micrometer-registry-prometheus

management.endpoints.web.exposure.include=health,prometheus,metrics
```

| Piece | Role |
|---|---|
| **Micrometer** | Facade — your code / Boot records metrics |
| **`/actuator/prometheus`** | Exposes metrics in Prometheus text format |
| **Prometheus** | Scrapes & stores time series (+ can alert) |
| **Grafana** | Dashboards / graphs (usually on top of Prometheus) |

### Are both Prometheus and Grafana needed?

**No — different jobs. Prometheus is the core; Grafana is the usual UI.**

| Tool | Needed? | Role |
|---|---|---|
| **Prometheus** | **Yes** (typical Spring path) | Collect / store metrics; scrape Actuator; alert rules |
| **Grafana** | **Optional but common** | Beautiful dashboards; often reads from Prometheus |

```text
App (Micrometer) → Prometheus (store + alert) → Grafana (visualize)
                         ↑
                    enough alone for many teams
```

| Setup | OK? |
|---|---|
| Prometheus only | Yes — query + alerts work |
| Prometheus + Grafana | Most common production combo |
| Actuator only (no Prom) | Fine for local peek; not long-term metrics storage |

**Interview line:**  
> “Prometheus is the metrics backend; Grafana is the dashboard. You need a store/scraper; Grafana is optional but usual for visualization.”

You’ve already seen related signals: Resilience4j CB metrics / Actuator on the Circuit Breaker demos.

---

## 5. Golden signals (SRE-style — useful pitch)

For a service, watch roughly:
1. **Latency**  
2. **Traffic** (QPS)  
3. **Errors**  
4. **Saturation** (CPU, threads, queue depth)

---

## 6. Metrics vs logs vs traces

| | Metrics | Logs | Traces |
|---|---|---|---|
| Best for | Trends, alerts | Detail / “why” text | One request’s path |
| Cost | Cheap to keep long | Expensive at volume | Sampled often |
| Example | p99 payment 800ms | “card declined” | Payment span 200ms |

**Flow in an incident:** Metric alert → open a **trace** for a slow request → read **logs** with that `traceId`.

---

## 7. Istio / Kiali (your project)

| Layer | What you get |
|---|---|
| **App (Micrometer)** | JVM, HTTP server/client, business counters |
| **Istio sidecars** | Request volume, success rate, latency **between** services |
| **Kiali** | Service graph + those traffic health views |

**Interview:**  
> “App metrics via Micrometer/Prometheus; mesh traffic metrics via Istio, visualized in Kiali. Same pillar — different source.”

Deep mesh + hop path → still mainly concept **05 (tracing)**; Kiali sits across **metrics + topology + traces**.

---

## 8. Pitch (15 seconds)

> “Metrics tell us how bad and how often. In Spring we use Micrometer and scrape `/actuator/prometheus` into Prometheus/Grafana for latency and error alerts. On OpenShift, Istio/Kiali add service-to-service traffic metrics. Alerts lead us to traces and logs for root cause.”

---

## Checkpoint

1. Counter vs gauge vs timer?  
2. Why Prometheus scrapes Actuator?  
3. Metrics vs traces in an incident?  
4. Where does Kiali fit?

### Answers

1. **Counter** = only increases (totals). **Gauge** = current value up/down. **Timer** = durations + count (latency).  
2. Actuator `/actuator/prometheus` **exposes** metrics; Prometheus **pulls/stores** them for query and alerts.  
3. **Metrics** alert “Payment p99 up”; **traces** show which hop for one request; then **logs** explain why.  
4. **Istio mesh traffic metrics + service graph** (platform view). App JVM/HTTP metrics still come from Micrometer.

When ready → concept **5: Distributed Tracing**.
