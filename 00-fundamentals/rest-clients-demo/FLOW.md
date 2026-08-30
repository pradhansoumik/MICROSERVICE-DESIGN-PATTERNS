# FLOW — REST client demos

```text
Browser / Postman
       │
       │  GET http://localhost:8300/api/{style}/products
       ▼
product-caller (:8300)
       │
       ├─ RestTemplate  ──HTTP──►  product-provider (:8301) GET /products
       ├─ RestClient    ──HTTP──►  product-provider (:8301) GET /products
       └─ FeignClient   ──HTTP──►  product-provider (:8301) GET /products
       │
       ▼
JSON: { calledVia: "...", fromProvider: { ... } }
```

## Per style (who runs)

| Style | Caller code | HTTP library path |
|---|---|---|
| RestTemplate | `restTemplate.getForObject(...)` | `RestTemplate` bean |
| RestClient | `restClient.get().uri(...).retrieve()` | `RestClient` bean |
| Feign | `feignClient.listProducts()` | Feign proxy → HTTP |

Same provider, same JSON — only the **client API** changes.

---

## Feign client workflow (detail)

Feign = you write an **interface**; Spring creates a **proxy** that turns method calls into **HTTP**.

### 1) Startup (once)

```text
@EnableFeignClients
        │
        ▼
Scan for @FeignClient interfaces
        │
        ▼
Find ProductFeignClient
  name = "productProvider"
  url  = http://localhost:8301   (from provider.base-url)
        │
        ▼
Create a proxy bean of type ProductFeignClient
(register in Spring context — injectable)
```

You never write an `implements` class.

### 2) Request time (each call)

```text
GET http://localhost:8300/api/feign/products
        │
        ▼
CallerController.viaFeign()
        │
        │  feignClient.listProducts()
        ▼
Feign proxy intercepts the method call
        │
        │  reads @GetMapping("/products")
        │  builds: GET http://localhost:8301/products
        ▼
HTTP call to product-provider
        │
        ▼
JSON → Map → controller wraps { calledVia: "OpenFeign", fromProvider: ... }
```

### 3) Picture

```text
You write:     interface ProductFeignClient { listProducts(); }

Spring makes:  Proxy ──HTTP──► product-provider:8301/products

Your code:     feignClient.listProducts()  ≈  GET /products
```

### 4) What each annotation does

| Piece | Role |
|---|---|
| `@EnableFeignClients` | Turn on scanning / proxy creation |
| `@FeignClient(..., url=...)` | Which host (demo: fixed URL) |
| `@GetMapping("/products")` | HTTP method + path |
| Method return type | How to decode the body (`Map`, DTO, …) |

### 5) With Eureka later (no `url`)

```text
@FeignClient(name = "PRODUCT-SERVICE")   // no url
        │
        ▼
Resolve PRODUCT-SERVICE via Eureka / K8s
        │
        ▼
Pick instance → HTTP
```

This demo skips that and uses `url = ${provider.base-url}`.

**Interview one-liner:**  
> “Feign is declarative: I define a `@FeignClient` interface with Spring MVC mappings. At runtime Spring injects a proxy that performs the HTTP call. Controllers just call the interface method like a local service.”

---

## Production note

Replace fixed `provider.base-url` with service discovery:

```text
@FeignClient(name = "PRODUCT-SERVICE")   // no url=
@LoadBalanced RestTemplate / RestClient
```

See `05-service-discovery`.
