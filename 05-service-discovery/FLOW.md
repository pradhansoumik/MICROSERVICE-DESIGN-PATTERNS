# FLOW — Service Discovery (Eureka)

Ports: Eureka **8761** · Storefront **8200** · Order **8201** · Product **8202**

---

## A) Registration (service → registry)

```text
ORDER-SERVICE starts (:8201)
  │
  │  spring.application.name=ORDER-SERVICE
  │  eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
  ▼
Eureka Server
  │
  └─ registry entry: ORDER-SERVICE → instance ip:8201

Same for PRODUCT-SERVICE → ip:8202
(Optional 2nd Product on :8203 → two instances under same name)
```

Heartbeat: client periodically renews lease; if it stops, Eureka marks instance down (after timeout).

---

## B) Discovery + call (client → service by name)

```text
Client (browser / Postman)
  │  GET http://localhost:8200/api/storefront/summary
  ▼
STOREFRONT-CLIENT
  │
  │  RestTemplate + @LoadBalanced
  │  get("http://ORDER-SERVICE/orders")
  │  get("http://PRODUCT-SERVICE/products")
  ▼
Spring Cloud LoadBalancer
  │  ask local Eureka cache: instances for PRODUCT-SERVICE?
  │  pick one (round-robin) → e.g. 192.168.x.x:8202
  ▼
HTTP to chosen instance /products
  ▼
JSON back → Storefront → Client
```

**Key idea:** URL uses **service name**, not `localhost:8202`.

---

### B2) Example - Behind `http://PRODUCT-SERVICE/products` (step-by-step)

### Step-by-step (what happens)

```text
1) Your code only sees a logical URL
   http://PRODUCT-SERVICE/products
         host = PRODUCT-SERVICE   path = /products
   Class: StorefrontController  (our code)

2) RestTemplate starts the request
   Class: RestTemplate
   Because bean is @LoadBalanced, an interceptor runs FIRST
   (plain RestTemplate would DNS-lookup "PRODUCT-SERVICE" and fail)

3) Interceptor intercepts before the wire call
   Class: LoadBalancerInterceptor  (ClientHttpRequestInterceptor)
   Wired onto RestTemplate by LoadBalancerAutoConfiguration
   (marker: @LoadBalanced)

4) Interceptor asks LoadBalancerClient to choose an instance
   Class: BlockingLoadBalancerClient  (implements LoadBalancerClient)
   "Give me an instance for service id = PRODUCT-SERVICE"

5) LoadBalancer gets instance list from discovery supplier
   Classes:
     RoundRobinLoadBalancer          (pick strategy, default)
     DiscoveryClientServiceInstanceListSupplier  (+ caching wrapper)
     EurekaDiscoveryClient           (Spring Cloud DiscoveryClient)
     → Netflix EurekaClient local cache
   Cache was filled / refreshed in background from Eureka Server :8761
   Example: PRODUCT-SERVICE → [ 192.168.1.10:8202 , 192.168.1.10:8203 ]

6) One instance is chosen (e.g. round-robin)
   → 192.168.1.10:8202
   Class: RoundRobinLoadBalancer (+ BlockingLoadBalancerClient.choose)

7) URL is rewritten to real host
   http://PRODUCT-SERVICE/products
        becomes
   http://192.168.1.10:8202/products
   Classes: BlockingLoadBalancerClient.reconstructURI / LoadBalancerUriTools
            then LoadBalancerInterceptor continues the request

8) Real HTTP GET on the wire
   Class: RestTemplate → ClientHttpRequest (JDK/SimpleClientHttpRequest…)
   Hits ProductController on that instance → JSON back
```

### Picture (with classes)

```text
StorefrontController                    ← our code
        │  get("http://PRODUCT-SERVICE/products")
        ▼
RestTemplate
        │
        ▼
LoadBalancerInterceptor                 ← @LoadBalanced hook
        │
        ▼
BlockingLoadBalancerClient
        │
        ├─► RoundRobinLoadBalancer      ← picks instance
        │         │
        │         ▼
        │   DiscoveryClientServiceInstanceListSupplier
        │         │
        │         ▼
        │   EurekaDiscoveryClient / EurekaClient cache
        │         ▲
        │         │ periodic fetch
        │   Eureka Server :8761
        │
        ▼  reconstructURI → http://ip:8202/products
RestTemplate HTTP call
        │
        ▼
PRODUCT-SERVICE  ProductController
        │
        ▼
JSON back up the chain
```

**Note:** This is **not** an API Gateway `GlobalFilter`. Gateway uses filters (`NettyRoutingFilter`, etc.). Here the hook is a **RestTemplate `ClientHttpRequestInterceptor`**.

**What does not happen on every call:** full registry download from Eureka for that one request — usually the **local Eureka client cache** is used.

### Classes cheat-sheet (say in interview)

| Piece | Class / API |
|---|---|
| Marker on bean | `@LoadBalanced` |
| RestTemplate hook | `LoadBalancerInterceptor` |
| Choose + rewrite URI | `BlockingLoadBalancerClient` |
| Pick strategy (default) | `RoundRobinLoadBalancer` |
| Instance list from discovery | `DiscoveryClientServiceInstanceListSupplier` |
| Eureka bridge | `EurekaDiscoveryClient` → Netflix `EurekaClient` |
| Auto-wiring interceptor | `LoadBalancerAutoConfiguration` |

### Short spoken version (30–40 seconds)

> “Our Storefront controller calls a `@LoadBalanced` RestTemplate. `LoadBalancerInterceptor` intercepts the call and uses `BlockingLoadBalancerClient` with `RoundRobinLoadBalancer`. Instances come from `DiscoveryClientServiceInstanceListSupplier` backed by `EurekaDiscoveryClient`’s local cache. The URI is rewritten to a real ip:port, then RestTemplate makes the actual HTTP call to ProductController.”

Config details for `@LoadBalanced` + `defaultZone` → **`LOADBALANCED-AND-EUREKA-CONFIG.md`**

---

## C) Without vs with discovery

| | Without | With (this demo) |
|---|---|---|
| URL | `http://localhost:8202/products` | `http://PRODUCT-SERVICE/products` |
| Scale Product to 2 ports | Update every caller | Eureka lists both; LB picks |
| Move host | Breaks callers | Registry updates |

---

## D) Where load balancing happens

```text
Client-side LB (this demo):
  Storefront + @LoadBalanced → chooses instance → calls it

Server-side LB (also common):
  Client → Nginx/Gateway/K8s Service → picks pod
```

Interview: Eureka + `@LoadBalanced` = **client-side** discovery + LB.  
K8s Service ClusterIP = often **server-side** LB + DNS discovery.

---

## E) Eureka UI check

Open `http://localhost:8761` → see `ORDER-SERVICE`, `PRODUCT-SERVICE`, `STOREFRONT-CLIENT` registered.

---

## F) Related design note

Gateway can also use discovery: `uri=lb://ORDER-SERVICE` (Spring Cloud Gateway + Eureka).  
This folder keeps a **plain RestTemplate client** so the discovery idea stays obvious.  
See API Gateway pattern for edge routing.
