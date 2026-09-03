# 01 — Communication Between Microservices

**Common question:** “How do microservices communicate?”

---

## 1. Two styles

| Style | How | Examples | Best for                                                                                                                                                                                  |
|---|---|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Synchronous** | Caller waits for response | REST, gRPC | Queries, user-facing request/response (Synchronous REST means: for that one request, the code waits for the downstream response — not that the whole app serves only one user at a time.) |
| **Asynchronous** | Fire event / message; no wait | Kafka, RabbitMQ | Workflows, fan-out, decoupling, peak load                                                                                                                                                 |

```text
Sync:   Order ──HTTP──► Inventory   (wait for 200)
Async:  Order ──event──► Kafka ──► Inventory, Notification, Analytics
```

**Pitch:**  
> “We use REST for request/response and Kafka for async workflows so producers don’t block on every consumer.”

---

## 2. Sync in detail (REST / gRPC)

| Point | What to say |
|---|---|
| REST | Simple, HTTP+JSON, ubiquitous in Spring |
| gRPC | Faster, contracts (protobuf), good internal high-QPS |
| Client | OpenFeign / RestClient / WebClient / RestTemplate (see §3) |
| Must add | **Timeouts**, retries (careful), **circuit breaker**, idempotency for writes |
| Discovery | Find instance: Eureka **or** K8s Service DNS (`http://order-service`) |

**Demo links:** `05-service-discovery` (`@LoadBalanced`), `01-circuit-breaker` (resilience on outbound call).

---

## 3. Ways to call REST in Spring (client types)

Interviewers often ask: “How do you invoke another service over REST?”

| Client | Style | Typical use | Notes |
|---|---|---|---|
| **`RestTemplate`** | Synchronous, imperative | Legacy / older Spring apps | Still works; **maintenance mode** — prefer RestClient for new code |
| **`RestClient`** | Synchronous, fluent API | **New sync** Spring 6.1+ / Boot 3.2+ default choice | Same sync model as RestTemplate, modern API |
| **OpenFeign (`@FeignClient`)** | Declarative interface | Microservices with Spring Cloud | Looks like a Java interface; good with Eureka / LoadBalancer |
| **`WebClient`** | Reactive / non-blocking | WebFlux, high concurrency | Not for classic blocking MVC unless you know the trade-offs |
| **HTTP Interface** (Spring 6) | Declarative + RestClient/WebClient | Newer alternative to Feign-style APIs | Proxy over RestClient or WebClient |

### Quick examples (shape only)

```java
// RestTemplate (classic)
restTemplate.getForObject("http://inventory-service/stock/{id}", Stock.class, id);

// RestClient (modern sync)
restClient.get().uri("http://inventory-service/stock/{id}", id).retrieve().body(Stock.class);

// OpenFeign
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    @GetMapping("/stock/{id}")
    Stock getStock(@PathVariable String id);
}

// WebClient (reactive)
webClient.get().uri("/stock/{id}", id).retrieve().bodyToMono(Stock.class);
```

### What to say in interview

| Prefer | When |
|---|---|
| **RestClient** | New **blocking** Spring Boot 3 apps |
| **OpenFeign** | Many internal APIs, Spring Cloud, want declarative clients + discovery |
| **WebClient** | Reactive stack already, or non-blocking I/O required |
| **RestTemplate** | Existing code; mention you’d migrate new calls to RestClient |

**Always with any of them:** base URL from config/discovery (`lb://` or service name), **timeouts**, and resilience (CB / retry) — not bare unlimited HTTP.

**With discovery:**

```text
@LoadBalanced RestTemplate / RestClient / WebClient
     or
@FeignClient(name = "ORDER-SERVICE")   // resolves via Eureka / K8s + LoadBalancer
```

Our demos: storefront used **`@LoadBalanced RestTemplate`**; Feign is the same idea with less boilerplate.

**Hands-on (no Eureka):** `rest-clients-demo/` — same provider called via RestTemplate, RestClient, and Feign. See `rest-clients-demo/DEMO.md`.

---

## 4. Async in detail (events)

| Point | What to say |
|---|---|
| Pub/Sub | One event → many consumers |
| Saga choreography | Business steps advance via events |
| Needs | Idempotent consumers, DLQ, often **Outbox** |
| Not for | “User clicked Pay and needs immediate confirm” alone — usually mix sync + async |

**Demo link:** flow note `rest-clients-demo/ASYNC-COMMUNICATION-FLOW.md` · code later `06-event-driven` + `02-saga/CHOREOGRAPHY.md`.

**gRPC (sync, not REST):** `grpc/GRPC-OVERVIEW-AND-FLOW.md` + `grpc/grpc-demo/`.

---

## 5. Where API Gateway fits

```text
External clients  →  API Gateway  →  internal services
                         │
                    auth, RL, routing
```

| Call type | Path |
|---|---|
| **Client → system** | Through **Gateway** (not every service public) |
| **Service → service** | Direct REST/gRPC or Kafka (**not** always via gateway) |

**Demo:** `03-api-gateway` (routing, JWT, rate limit).

---

## 6. Patterns interviewers mix into “communication”

| Pattern | Role in communication |
|---|---|
| API Gateway | Edge entry, cross-cutting |
| Service Discovery | Resolve who to call |
| Circuit Breaker / Retry / Timeout | Survive bad dependencies |
| Saga / Events | Multi-service workflows without 2PC |

---

## 7. Decision cheat sheet

| Need | Prefer |
|---|---|
| User waits for answer | Sync REST/gRPC |
| Notify many systems | Async event |
| Strong immediate consistency of one resource | Sync + local DB TX |
| Long workflow across services | Async + Saga |
| External API | Gateway |

---

## 8. Interview Q&A

**Q: How do your services talk?**  
A: Sync REST for queries/commands that need a response; Kafka for domain events and choreography. Gateway for external traffic; internal S2S with discovery/K8s DNS and Resilience4j.

**Q: RestTemplate vs Feign vs WebClient vs RestClient?**  
A: RestTemplate = classic sync (legacy). RestClient = modern sync. Feign = declarative HTTP interface, great with Spring Cloud. WebClient = reactive/non-blocking. New blocking apps: RestClient or Feign; reactive: WebClient.

**Q: Sync vs async?**  
A: Sync = simpler, tighter coupling in time. Async = loose coupling, better scale, eventual consistency and more ops complexity.

**Q: Always use the gateway for S2S?**  
A: No — gateway is for **north-south** (clients). **East-west** is direct or mesh.

**Q: How avoid cascade failure?**  
A: Timeouts, CB, bulkhead, retries with backoff + idempotency — see circuit breaker demo.
