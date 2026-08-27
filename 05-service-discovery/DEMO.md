# Demo — Service Discovery

## Start order (4 terminals)

```powershell
# 1) Registry first
cd ...\05-service-discovery\eureka-server
mvn spring-boot:run

# 2) Order
cd ...\05-service-discovery\order-service
mvn spring-boot:run

# 3) Product
cd ...\05-service-discovery\product-service
mvn spring-boot:run

# 4) Storefront caller
cd ...\05-service-discovery\storefront-client
mvn spring-boot:run
```

Wait ~10–20s after services start so Eureka registration settles.

---

## Step 1 — Eureka dashboard

Open: http://localhost:8761  

Expect applications: `ORDER-SERVICE`, `PRODUCT-SERVICE`, `STOREFRONT-CLIENT`.

---

## Step 2 — Call via service names (through storefront)

```powershell
Invoke-RestMethod http://localhost:8200/api/storefront/summary
```

Expect JSON with both `orders` and `products`.  
Note `instancePort` under products (8202).

Direct (still works — bypasses discovery):
```powershell
Invoke-RestMethod http://localhost:8201/orders
Invoke-RestMethod http://localhost:8202/products
```

---

## Step 3 — Load balancing (2 Product instances)

Keep first Product on **8202**. New terminal:

```powershell
cd ...\05-service-discovery\product-service
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8203"
```

Refresh Eureka UI → `PRODUCT-SERVICE` shows **2** instances.

Hit products via storefront several times:

```powershell
1..6 | ForEach-Object {
  (Invoke-RestMethod http://localhost:8200/api/storefront/products).instancePort
}
```

Expect `instancePort` to alternate **8202 / 8203** (round-robin).

---

## Step 4 — What to say while demoing

1. Services **register**; client uses **logical name**  
2. `@LoadBalanced` RestTemplate resolves name via Eureka  
3. Extra instance → no code change on storefront  
4. Production alternative: **Kubernetes DNS** (`product-service.default.svc.cluster.local`)

---

## Troubleshooting

| Issue | Fix |
|---|---|
| Storefront 500 / UnknownHost PRODUCT-SERVICE | Wait for Eureka; check dashboard; ensure `@LoadBalanced` bean |
| Empty Eureka apps | Confirm `defaultZone=http://localhost:8761/eureka/` |
| Port in use | Change `server.port` or stop old process |
