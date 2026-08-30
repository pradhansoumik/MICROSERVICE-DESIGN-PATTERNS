# REST Clients Demo — RestTemplate / RestClient / Feign

**Goal:** Same call to a dummy provider, three Spring REST client styles — learn **one by one**.

| App | Port | Role |
|---|---|---|
| `product-provider` | **8301** | Dummy Product API |
| `product-caller` | **8300** | Calls provider three ways |

No Eureka — `provider.base-url=http://localhost:8301` for simplicity.  
Discovery/`lb://` is covered in `05-service-discovery`.

---

## Learn order

1. **RestTemplate** → `GET /api/rest-template/products`  
2. **RestClient** → `GET /api/rest-client/products`  
3. **OpenFeign** → `GET /api/feign/products`  

Theory: `../01-SERVICE-COMMUNICATION.md` §3

---

## Why choose which? (summary)

### RestClient **over** RestTemplate

| | RestTemplate | RestClient |
|---|---|---|
| Status | Older; **maintenance mode** | **Recommended** sync client (Spring 6.1+) |
| API | Verbose (`getForObject`, exchange…) | Fluent, readable (`get().uri().retrieve()`) |
| New code | Avoid for greenfield | Prefer for **blocking** MVC apps |

**Choose RestClient** for new synchronous calls — same job as RestTemplate, modern API, Spring’s direction forward.

**Keep RestTemplate** only when maintaining existing code (no rush to rewrite everything).

---

### FeignClient **over** RestClient

| | RestClient | OpenFeign (`@FeignClient`) |
|---|---|---|
| Style | You write HTTP calls in code | **Declarative** Java interface |
| Boilerplate | URI + method in each service class | One interface; Spring implements it |
| Spring Cloud | Works with `@LoadBalanced` | Natural fit with **Eureka / LoadBalancer** (`name = "SERVICE"`) |
| Many endpoints | Can get repetitive | Clean when you have **many** provider APIs |

**Choose Feign** when you want service clients as interfaces, lots of endpoints, and Spring Cloud discovery (no hardcoded URLs in call sites).

**Choose RestClient** when you want explicit control, fewer dependencies (no OpenFeign), or a one-off call without a client interface.

```text
Simple / modern sync, few calls     → RestClient
Many internal APIs + Spring Cloud   → Feign
Legacy codebase                     → RestTemplate (until migrated)
```

---

## Docs

| File | Purpose |
|---|---|
| `DEMO.md` | Run + curl/PowerShell |
| `FLOW.md` | Request path per client |

---

## Interview one-liner

> “RestTemplate is the classic blocking client; RestClient is the modern sync replacement; Feign is a declarative interface over HTTP — useful with Spring Cloud. All three can call the same REST API; we still add timeouts and resilience in production.”
