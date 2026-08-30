# 06 — Pitch & Checklist

**Goal:** One story you can say in an interview, plus a quick revise list.  
**Your POC:** OpenSearch (logs/traces), Istio/Kiali, Actuator on OCP.

---

## 1. 60–90 second pitch (memorize)

> “Observability for microservices is **logs, metrics, and traces**.  
> **Health** with Spring Actuator feeds OpenShift **liveness/readiness** so only ready pods get traffic.  
> **Logs** are structured with **requestId/traceId** in MDC; we migrated **Elasticsearch → OpenSearch** and search incidents in OpenSearch Dashboards.  
> **Metrics** come from **Micrometer**; Prometheus scrapes `/actuator/prometheus`, Grafana for dashboards — Prometheus stores/alerts, Grafana visualizes. Without a mesh we still need this; with **Istio**, **Kiali** adds service-to-service traffic metrics and topology.  
> **Traces** use one **traceId** and a **span** per hop; context propagates via **traceparent** (OTel/Istio). We view traces on OpenSearch Dashboards.  
> In an incident: **metrics alert → open a trace → logs with the same traceId**.”

---

## 2. One diagram (all pillars)

```text
                         Client request
                               │
                               ▼
                     API Gateway (+ ids)
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
           LOGS            METRICS           TRACES
        OpenSearch        Prometheus        OpenSearch /
        Dashboards        (+ Grafana)       Jaeger / Zipkin
              ▲                ▲                ▲
              │                │                │
         MDC fields      Micrometer        spans + OTLP
         requestId       /actuator/        or Istio
         traceId         prometheus
                               │
                        Istio / Kiali
                     (mesh traffic view)
```

---

## 3. Quick revise checklist

### Concepts
- [ ] Observability vs monitoring  
- [ ] Three pillars + health’s role  
- [ ] Liveness vs readiness  
- [ ] requestId vs traceId vs spanId  
- [ ] MDC = filter once per service; not every controller param  
- [ ] Propagate headers (or OTel/Istio) across S2S calls  
- [ ] Counter / gauge / timer  
- [ ] Prometheus vs Grafana (store vs UI)  
- [ ] OTLP = export protocol to collector/backends  
- [ ] Incident order: metrics → trace → logs  

### Your project lines
- [ ] ES → OpenSearch for logging  
- [ ] Traces on OpenSearch Dashboards  
- [ ] Istio + Kiali for mesh  
- [ ] Actuator probes on OCP  

### Repo notes to skim
| Doc | Use |
|---|---|
| `01` … `05` | Pillar deep dives |
| `LOG-AND-TRACING-FLOW.md` | Visual Pay → Gateway → Order → Payment |
| `REAL-PROJECT-MAPPING.md` | POC → which section |

---

## 4. Likely interview Qs (short answers)

**Q: Three pillars?**  
A: Logs, metrics, traces (+ health/probes for K8s).

**Q: Liveness vs readiness?**  
A: Restart vs don’t send traffic.

**Q: Why MDC?**  
A: Put correlation ids on every log without passing headers into every method.

**Q: Prometheus without Actuator?**  
A: Something else must expose a scrape endpoint (custom Micrometer endpoint, OTel, exporter) or Prom has nothing to scrape.

**Q: Kiali replace Grafana?**  
A: No — Kiali is mesh traffic/topology; Grafana usually app/Prom metrics.

**Q: OTLP?**  
A: OpenTelemetry’s wire protocol to send traces/metrics/logs to a collector/backend.

---

## 5. What’s left in the big roadmap

| Done here | Still later |
|---|---|
| Observability concepts 01–06 | Optional small Spring demo |
| | **06 Event-Driven** after Kafka learning |

---

Observability concept series: **complete**. Skim the checklist before interviews; say if you want a tiny Actuator/Prometheus demo next.
