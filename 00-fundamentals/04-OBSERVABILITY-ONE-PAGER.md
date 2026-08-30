# 04 — Observability One-Pager

**Common question:** “How do you monitor / debug microservices?”

---

## Three pillars + health

| Pillar | Tooling (your world) |
|---|---|
| **Logs** | OpenSearch (ES → OS migration) + correlation ids |
| **Metrics** | Micrometer → Prometheus → Grafana; Istio/Kiali for mesh traffic |
| **Traces** | OpenSearch Dashboards / OTel; Istio propagation |
| **Health** | Actuator liveness/readiness on OCP |

**Incident habit:** metrics alert → open **trace** → **logs** with same `traceId`.

**Full notes:** `07-observability/` (concepts 01–06 + `LOG-AND-TRACING-FLOW.md`).

**Pitch:**  
> “Logs, metrics, traces, plus K8s probes. We use OpenSearch for logs/traces and Istio/Kiali for mesh; Actuator for readiness/liveness.”
