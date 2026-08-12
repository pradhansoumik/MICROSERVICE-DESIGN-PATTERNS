# Demo cheat-sheet — Saga (port 8090)

## Start
```powershell
cd D:\planning-preparation-Execution\MICROSERVICES_NOTES\GITHUB_CHECKIN\MICROSERVICE-DESIGN-PATTERNS\02-saga\saga-demo
mvn spring-boot:run
```

## Step A — Happy path (COMPLETED)
```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8090/api/saga/failure-mode?mode=NONE"

Invoke-RestMethod -Method POST -Uri http://localhost:8090/api/saga/orders `
  -ContentType "application/json" `
  -Body '{"customerId":"CUST-1","productId":"SKU-100","quantity":1,"amount":999.0}'
```

**Expect:** `status=COMPLETED`, timeline includes `ORDER_CREATED` → `INVENTORY_RESERVED` → `PAYMENT_COMPLETED` → `ORDER_CONFIRMED`

Optional snapshot (replace sagaId):
```powershell
Invoke-RestMethod http://localhost:8090/api/saga/SAGA-XXXX/snapshot
```
orderStatus=CONFIRMED, reservation=RESERVED, payment=CAPTURED

---

## Step B — Payment fails → compensate
```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8090/api/saga/failure-mode?mode=PAYMENT"

Invoke-RestMethod -Method POST -Uri http://localhost:8090/api/saga/orders `
  -ContentType "application/json" `
  -Body '{"customerId":"CUST-1","productId":"SKU-100","quantity":1,"amount":999.0}'
```

**Expect:** `status=COMPENSATED`  
Timeline includes:
- `INVENTORY_RESERVED`
- `STEP_FAILED:...`
- `COMPENSATE_INVENTORY_RELEASE`
- `COMPENSATE_ORDER_CANCEL`

**Logs:** payment error, then release inventory, cancel order.  
No money captured (`paymentId` usually null).

Snapshot: orderStatus=CANCELLED, reservationStatus=RELEASED

---

## Step C — Inventory fails (earlier step)
```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8090/api/saga/failure-mode?mode=INVENTORY"

Invoke-RestMethod -Method POST -Uri http://localhost:8090/api/saga/orders `
  -ContentType "application/json" `
  -Body '{"customerId":"CUST-1","productId":"SKU-100","quantity":1,"amount":999.0}'
```

**Expect:** compensate only **order cancel** (inventory never reserved → no release).  
Interview point: *compensate only what succeeded*.

---

## Reset to happy
```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8090/api/saga/failure-mode?mode=NONE"
```
