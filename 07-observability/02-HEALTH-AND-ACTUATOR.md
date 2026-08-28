# 02 — Health & Actuator

**Pillar link:** Not one of the classic “3 pillars,” but **day-1 ops** — especially on **Kubernetes / OCP**.  
**Spring tool:** Spring Boot **Actuator**.

---

## 1. What problem does Health solve?

| Question | Who asks it? |
|---|---|
| Is this process alive? | Orchestrator / you |
| Is it ready to take traffic? | K8s / OCP / load balancer |
| Are dependencies OK? (DB, disk) | Ops / readiness logic |

Without health:
- K8s may send traffic to a pod that is still starting or broken  
- Load balancer keeps a dead instance in rotation  

---

## 2. Liveness vs Readiness (must know for OCP)

| Probe | Meaning | If it fails |
|---|---|---|
| **Liveness** | “Should this container be **restarted**?” | Kubelet **restarts** the pod |
| **Readiness** | “Can this pod take **traffic**?” | Removed from Service endpoints (no restart by itself) |

**Examples**
- App deadlocked → **liveness** fail → restart  
- App up but DB not ready yet → **readiness** fail → no traffic until DB OK  

**Interview line:**  
> “Liveness = kill/restart me. Readiness = don’t send me traffic yet.”

---

## 3. Spring Boot Actuator (what you enable)

Dependency: `spring-boot-starter-actuator`

Common endpoints:

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | Aggregated health (UP / DOWN) |
| `/actuator/health/liveness` | For K8s liveness probe (Boot 2.3+) |
| `/actuator/health/readiness` | For K8s readiness probe |
| `/actuator/info` | Build/app info |
| `/actuator/metrics` | Metric names (next concept goes deeper) |
| `/actuator/prometheus` | Prometheus scrape format (needs micrometer-registry-prometheus) |

Expose carefully in prod (security!). Demo often:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when_authorized
# or for local learning:
# management.endpoint.health.show-details=always

management.endpoint.health.probes.enabled=true
```

---

## 4. What “UP” means

`/actuator/health` can include **indicators**:
- Disk space  
- DB (if DataSource on classpath)  
- Custom checks (e.g. “can reach payment”)  
- Resilience4j circuit breakers (you’ve seen CB health contributions)

Overall status is typically **DOWN** if a critical indicator is DOWN (depending on config).

---

## 5. Flow on OpenShift / K8s

```text
Pod starts
   │
   ▼
Liveness probe  → GET /actuator/health/liveness
   │  fail often → restart
   ▼
Readiness probe → GET /actuator/health/readiness
   │  fail → not in Service Endpoints (no user traffic)
   │  pass → Service can route to this pod
   ▼
App serves business APIs
```

Same idea as Service Discovery note: **Ready pods** are the ones that receive traffic.

---

## 6. How this ties to Observability

| Signal | Role |
|---|---|
| Health | **Binary / coarse** — up or not, ready or not |
| Metrics | Trends — error rate, latency (concept 4) |
| Logs / Traces | Detail of a failure (concepts 3 & 5) |

Health answers “should traffic go here?”  
It does **not** replace metrics/traces for “why is checkout slow?”

---

## 7. Mini practice (no new project required)

Any of your existing Boot apps with Actuator (CB order-service, gateway, Eureka clients):

```text
GET http://localhost:<port>/actuator/health
GET http://localhost:<port>/actuator/health/liveness
GET http://localhost:<port>/actuator/health/readiness
```

If probes aren’t enabled, enable:

```properties
management.endpoint.health.probes.enabled=true
```

---

## 8. Pitch (15 seconds)

> “We expose Spring Actuator health and map Kubernetes liveness and readiness probes to `/actuator/health/liveness` and `/readiness`. That way OpenShift only sends traffic to ready pods and restarts stuck ones. Health is the coarse gate; logs, metrics, and traces explain failures in depth.”

---

## Checkpoint

1. Liveness vs readiness?  
2. Why Actuator on OCP?  
3. Is health enough for full observability? (**No** — need logs/metrics/traces too.)

When ready → concept **3: Logging & correlation**.
