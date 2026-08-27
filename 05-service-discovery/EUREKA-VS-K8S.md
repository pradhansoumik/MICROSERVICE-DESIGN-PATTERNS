# Eureka vs Kubernetes (OCP) — short note

**Demo (this folder):** Eureka + `@LoadBalanced` RestTemplate  
**Your production (typical):** OpenShift / Kubernetes Service + DNS

---

## Side-by-side

| | **Eureka** (demo) | **Kubernetes / OCP** (production default) |
|---|---|---|
| **Register** | App Eureka client: “I am PRODUCT-SERVICE on ip:port” + heartbeats | Deployment pods + **Service** selector → Endpoints (Ready pods) |
| **Discover** | Caller fetches/caches registry; resolves **service name** | Caller uses **DNS**: `http://product-service:8080` |
| **Load balance** | **Client-side** (`@LoadBalanced` / LoadBalancer) | **Platform** (Service → pod) |
| **Extra ops** | Run HA Eureka cluster | Already built into the cluster |
| **App URL style** | `http://PRODUCT-SERVICE/products` | `http://product-service:8080/products` |

---

## Register & discover (K8s picture)

```text
REGISTER                              DISCOVER
────────                              ────────
Deployment → Pods                     order-service
      │                                     │
      │ labels                              │ http://product-service
      ▼                                     ▼
Service + Endpoints                     CoreDNS → ClusterIP
product → pod IPs                            │
                                             ▼
                                        product Pod
```

---

## What to choose & why

| Environment | Choose | Why |
|---|---|---|
| **OCP / Kubernetes** | **K8s Service + DNS** | Native, no extra registry, LB included |
| **Learning / classic Spring Cloud on VMs** | Eureka | Clear client-side discovery demo |
| **Legacy hybrid** | Maybe Eureka (or Consul) | Only if apps outside the cluster still depend on it |

**Default for your real project on OCP:** Kubernetes discovery — **not** Eureka.

---

## App changes if you move demo → K8s

- Remove Eureka client / `defaultZone` (if only used for discovery)  
- Call Service DNS name (from config/env), not `localhost`  
- Drop `@LoadBalanced` for those in-cluster calls  
- Expose Actuator **liveness/readiness** for probes  

Business APIs stay the same.

---

## Interview pitch

> “Eureka teaches client-side discovery: apps register and callers resolve names with a load-balanced client. On OpenShift we use Kubernetes: a Service selects Ready pods, DNS resolves the Service name, and the platform load-balances. We keep a gateway/route at the edge; inside the cluster, Service DNS is enough.”

---

## Related

| File | Purpose |
|---|---|
| `FLOW.md` | Eureka call path + classes |
| `LOADBALANCED-AND-EUREKA-CONFIG.md` | `@LoadBalanced` + config |
| `SERVICE-DISCOVERY_INTERVIEW-REVISION-NOTES.md` | Broader Q&A |
