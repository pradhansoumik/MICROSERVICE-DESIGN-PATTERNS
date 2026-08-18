# Demo cheat-sheet — CQRS (port 8091)

## Start
```powershell
cd D:\planning-preparation-Execution\MICROSERVICES_NOTES\GITHUB_CHECKIN\MICROSERVICE-DESIGN-PATTERNS\04-cqrs\cqrs-demo
mvn spring-boot:run
```

## Step 1 — Command: place order (WRITE)
```powershell
$place = Invoke-RestMethod -Method POST -Uri http://localhost:8091/api/commands/orders `
  -ContentType "application/json" `
  -Body '{"customerId":"CUST-1","productId":"SKU-100","quantity":2,"amount":1998.0}'
$place
$orderId = $place.orderId
```

**Expect:** `side = COMMAND / WRITE`, `status = CREATED`

## Step 2 — Query: get order (READ)
```powershell
Invoke-RestMethod http://localhost:8091/api/queries/orders/$orderId
```

**Expect:** `side = QUERY / READ`, includes `displaySummary` (denormalized)

## Step 3 — Query: list by customer
```powershell
Invoke-RestMethod "http://localhost:8091/api/queries/orders?customerId=CUST-1"
```

## Step 4 — Command: cancel, then query again
```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8091/api/commands/orders/$orderId/cancel"
Invoke-RestMethod http://localhost:8091/api/queries/orders/$orderId
```

**Expect:** read model `status = CANCELLED` after projection

## What to say while demoing
1. POST goes to **command** side only  
2. GET goes to **query** side only  
3. Projector copies write → read (in prod: Kafka/CDC)
