# 05 — Service Discovery

**Category:** Communication / location transparency  
**Real-life:** Storefront app must call Order + Product services without hard-coding `host:port`. When Product scales to 2 instances, calls still work and are load-balanced.

| App | Port | Role |
|---|---|---|
| `eureka-server` | **8761** | Service registry (phone book) |
| `order-service` | 8201 | Registers as `ORDER-SERVICE` |
| `product-service` | 8202 (optional 8203) | Registers as `PRODUCT-SERVICE` |
| `storefront-client` | **8200** | Caller using **service names** + `@LoadBalanced` |

---

## 1. Problem

Without discovery:
- Every caller hard-codes `localhost:8201`, `localhost:8202`  
- Scaling / moving a service breaks callers  
- No automatic instance list for load balancing  

**Service Discovery** = services register themselves; callers look up by **logical name**.

---

## 2. Architecture

```text
                    ┌─────────────────┐
                    │  Eureka Server  │ :8761
                    │   (registry)    │
                    └────────▲────────┘
           register │        │ register / fetch
    ┌───────────────┴──┐  ┌──┴────────────────┐
    │  ORDER-SERVICE   │  │ PRODUCT-SERVICE   │
    │     :8201        │  │  :8202 (+ :8203)  │
    └────────▲─────────┘  └─────────▲─────────┘
             │                      │
             │   resolve by name    │
             └──────────┬───────────┘
                        │
              ┌─────────┴──────────┐
              │ STOREFRONT-CLIENT  │ :8200
              │ @LoadBalanced RT   │
              └────────────────────┘
```

---

## 3. How it works (say this)

1. Order & Product start → **register** with Eureka (`spring.application.name`)  
2. Storefront starts → also a Eureka client → **fetches** registry  
3. Call `http://PRODUCT-SERVICE/products`  
4. Load balancer replaces name with a real `ip:port` from Eureka  
5. HTTP goes to that instance  

---

## 4. Docs in this folder

| File | Purpose |
|---|---|
| `FLOW.md` | Register → lookup → load balance → response |
| `DEMO.md` | Run steps + 2-instance LB demo |
| `SERVICE-DISCOVERY_INTERVIEW-REVISION-NOTES.md` | Eureka vs K8s DNS, client vs server LB |

---

## 5. 90-second pitch

> “Services register with a registry (Eureka in this demo). Callers use logical names like PRODUCT-SERVICE with a load-balanced client; the client resolves healthy instances from the registry. That removes hard-coded hosts and supports scaling. In Kubernetes, DNS often replaces a separate Eureka registry.”
