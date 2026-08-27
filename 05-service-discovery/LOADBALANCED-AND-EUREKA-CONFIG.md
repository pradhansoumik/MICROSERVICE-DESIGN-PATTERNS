# @LoadBalanced + Eureka config — what each piece does

**Demo:** `storefront-client` calls `http://PRODUCT-SERVICE/...`  
**Goal:** Remember *why* `@LoadBalanced` exists and which properties support it.

---

## 1. Main significance of `@LoadBalanced`

**One job:** Make `RestTemplate` (or WebClient) treat the URL host as a **service id**, not a DNS hostname.

```java
@Bean
@LoadBalanced
RestTemplate loadBalancedRestTemplate() {
    return new RestTemplate();
}

// host "PRODUCT-SERVICE" → resolve via Eureka cache → pick instance → real HTTP
restTemplate.getForObject("http://PRODUCT-SERVICE/products", Map.class);
```

| Without `@LoadBalanced` | With `@LoadBalanced` |
|---|---|
| JVM DNS lookup for `PRODUCT-SERVICE` | Look up instances in **local Eureka cache** |
| Usually `UnknownHostException` | Rewrite to `http://ip:port/products` + call |
| No instance picking | Load balancer picks one instance (e.g. round-robin) |

**Interview line:**  
> “Eureka gives the instance list. `@LoadBalanced` makes RestTemplate **use** that list to resolve the service name and choose an instance.”

`@LoadBalanced` does **not** fetch the registry by itself.  
Eureka client fetches/caches; `@LoadBalanced` **consumes** that cache on each call.

---

## 2. Supportive configuration (purpose of each)

### On every Eureka **client** (Order, Product, Storefront)

```properties
spring.application.name=PRODUCT-SERVICE
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true
```

| Property | Purpose |
|---|---|
| `spring.application.name` | **Service id** in the registry (what callers use in the URL) |
| `eureka.client.service-url.defaultZone` | **Where Eureka server is** — register + fetch registry from here |
| `eureka.instance.prefer-ip-address=true` | Register with IP (handy on local/Docker; avoids bad hostname resolution) |

### On Eureka **server** only

```properties
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

| Property | Purpose |
|---|---|
| `register-with-eureka=false` | Server is the phone book — don’t register itself |
| `fetch-registry=false` | Server doesn’t need to pull other registries (standalone) |

### On **Storefront** (caller) — Maven + bean

| Piece | Purpose |
|---|---|
| `spring-cloud-starter-netflix-eureka-client` | Fetch/cache registry + (optional) register client |
| `spring-cloud-starter-loadbalancer` | Actual LB that `@LoadBalanced` hooks into |
| `@LoadBalanced` on `RestTemplate` | Resolve `http://SERVICE-NAME/...` via that LB |

Providers (Order/Product) need Eureka client to **register**.  
Caller needs Eureka client to **fetch** + LoadBalancer + `@LoadBalanced` to **resolve**.

---

## 3. Flow (config → runtime)

```text
┌─ STARTUP ─────────────────────────────────────────────┐
│                                                       │
│  PRODUCT-SERVICE                                      │
│    name=PRODUCT-SERVICE                               │
│    defaultZone → http://localhost:8761/eureka/        │
│         │                                             │
│         ▼  REGISTER                                   │
│    Eureka Server stores: PRODUCT-SERVICE → :8202      │
│                                                       │
│  STOREFRONT-CLIENT                                    │
│    defaultZone → same Eureka URL                      │
│         │                                             │
│         ▼  FETCH (periodic)                           │
│    Local cache: PRODUCT-SERVICE → [:8202, :8203?]     │
│                                                       │
│  @LoadBalanced RestTemplate bean ready                │
└───────────────────────────────────────────────────────┘

┌─ EACH REQUEST ────────────────────────────────────────┐
│                                                       │
│  Code: get("http://PRODUCT-SERVICE/products")         │
│         │                                             │
│         ▼                                             │
│  @LoadBalanced interceptor                            │
│         │  read local Eureka cache (not every time    │
│         │  a full HTTP round-trip to Eureka)          │
│         ▼                                             │
│  LoadBalancer picks instance → ip:8202                │
│         │                                             │
│         ▼                                             │
│  Real call: http://ip:8202/products                   │
│         │                                             │
│         ▼                                             │
│  Response back to Storefront → Client                 │
└───────────────────────────────────────────────────────┘
```

---

## 4. Split reminder (don’t mix these up)

| Concern | Who handles it |
|---|---|
| “Where is the registry?” | `eureka.client.service-url.defaultZone` |
| “What is my name?” | `spring.application.name` |
| “Keep a phone book locally” | Eureka **client** (background fetch) |
| “Use phone book on this HTTP call” | **`@LoadBalanced`** + LoadBalancer |

---

## 5. Related demos / notes

| Topic | Path |
|---|---|
| Run steps | `DEMO.md` |
| Full pattern flow | `FLOW.md` |
| Interview Q&A | `SERVICE-DISCOVERY_INTERVIEW-REVISION-NOTES.md` |
| Code | `storefront-client` → `StorefrontClientApplication` + `StorefrontController` |
