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
