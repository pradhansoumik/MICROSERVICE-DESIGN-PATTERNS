# Demo cheat-sheet — API Gateway

## Start (3 apps)
1. `order-backend` → 8101  
2. `product-backend` → 8102  
3. `api-gateway` → 8080  

## Step 1 — Products via Gateway
```powershell
Invoke-RestMethod http://localhost:8080/api/products
```
**Expect:** `service=product-backend`, `port=8102`, `receivedFromGateway=api-gateway`, and a `receivedRequestId`

## Step 2 — One product
```powershell
Invoke-RestMethod http://localhost:8080/api/products/SKU-100
```

## Step 3 — Create order via Gateway
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8080/api/orders `
  -ContentType "application/json" `
  -Body '{"customerId":"CUST-1","productId":"SKU-100","amount":999}'
```
**Expect:** `service=order-backend`, `port=8101`, `status=CREATED`

## Step 4 — Get order via Gateway
```powershell
Invoke-RestMethod http://localhost:8080/api/orders/ORD-DEMO1
```

## Step 5 — Prove Gateway adds request id
```powershell
Invoke-RestMethod http://localhost:8080/api/products -Headers @{ "X-Request-Id" = "REQ-INTERVIEW" }
```
**Expect:** `receivedRequestId=REQ-INTERVIEW`

## Optional — call backend directly (not recommended in prod)
```powershell
Invoke-RestMethod http://localhost:8102/products
```
Compare: no `X-Gateway` unless you add it yourself.

## Actuator
```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/actuator/gateway/routes
```
