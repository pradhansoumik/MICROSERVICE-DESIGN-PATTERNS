# Service Discovery — Interview Revision Notes

**Demo:** Eureka `:8761` + Order `:8201` + Product `:8202` + Storefront `:8200`  
**Code highlight:** `@LoadBalanced RestTemplate` calling `http://PRODUCT-SERVICE/...`

---

## 1. What problem does it solve?

Microservices change host/port and scale to N instances.  
Callers should use a **stable name**, not brittle `ip:port` lists.

---

## 2. Main pieces

| Piece | Role |
|---|---|
| **Registry** (Eureka) | Phone book of service → instances |
| **Service (provider)** | Registers + heartbeats |
| **Client (consumer)** | Fetches registry / resolves name |
| **Load balancer** | Picks one healthy instance |

---

## 3. Client-side vs server-side discovery

| | Client-side (this demo) | Server-side |
|---|---|---|
| Who picks instance? | Caller (LoadBalancer) | LB / K8s Service / Gateway |
| Client needs | Registry data | Only one VIP / DNS name |
| Example | Eureka + `@LoadBalanced` | Nginx, AWS ALB, K8s Service |

---

## 4. Eureka vs Consul vs Kubernetes DNS

| Tool | Typical use |
|---|---|
| **Eureka** | Spring Cloud classic registry (demo) |
| **Consul** | Registry + KV + health (HashiCorp) |
| **K8s DNS** | `my-svc.namespace.svc.cluster.local` — very common in prod |

**Pitch:** “Eureka teaches the pattern. On Kubernetes I’d often rely on Service DNS and let kube-proxy/Ingress do LB.”

---

## 5. Important Spring bits

```properties
spring.application.name=PRODUCT-SERVICE
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

```java
@EnableEurekaServer   // registry app
@EnableDiscoveryClient // optional on Boot 3 if starter present; still fine to show intent

@Bean
@LoadBalanced
RestTemplate restTemplate() { ... }

restTemplate.getForObject("http://PRODUCT-SERVICE/products", Map.class);
```

Without `@LoadBalanced`, `PRODUCT-SERVICE` is treated as a normal hostname → fails.

Gateway variant: `uri=lb://PRODUCT-SERVICE` (needs discovery + loadbalancer on gateway).

---

## 6. Health & self-preservation (awareness)

- Clients send **heartbeats**; missed heartbeats → instance removed (after thresholds)  
- Eureka **self-preservation**: if many heartbeats missing, Eureka may stop evicting (network partition protection). Demo disables it for clearer local behavior.

---

## 7. Common interview Q&A

**Q: Discovery vs API Gateway?**  
A: Discovery = find instances. Gateway = single entry, auth, routing, RL. Often **together**: Gateway uses `lb://SERVICE`.

**Q: Discovery vs Load Balancer?**  
A: Discovery answers “which instances exist?”. LB answers “which one do I call now?”. Often paired.

**Q: What if Eureka is down?**  
A: Clients cache registry; stale data risk. HA Eureka cluster / or use K8s DNS. Discuss trade-offs.

**Q: Service mesh?**  
A: Istio/Linkerd push discovery + LB + mTLS + retries to sidecar — advanced follow-up.

---

## 8. 30-second pitch

> “I use a service registry so providers register and consumers call by logical name. In the Spring demo that’s Eureka plus a load-balanced RestTemplate. That enables scaling without changing callers. In Kubernetes, DNS-based discovery often replaces a separate Eureka cluster.”
