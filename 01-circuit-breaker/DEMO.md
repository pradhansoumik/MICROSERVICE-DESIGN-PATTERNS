# Demo cheat-sheet — Circuit Breaker

## Start
1. payment-service → port 8081
2. order-service → port 8082

## Commands (PowerShell)

### Place order
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8082/api/orders -ContentType "application/json" -Body '{"customerId":"CUST-1","productId":"SKU-100","quantity":1,"amount":999.0}'
```

### Circuit status
```powershell
Invoke-RestMethod http://localhost:8082/api/orders/circuit-status
```

### Break payment
```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8081/api/payments/failure-mode?mode=FAIL"
```

### Heal payment
```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8081/api/payments/failure-mode?mode=NONE"
```

### Slow payment
```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8081/api/payments/failure-mode?mode=SLOW"
```

## Expected story
NONE → CONFIRMED / CLOSED  
FAIL (several calls) → PENDING_PAYMENT / **OPEN** (check status right away!)  
Wait 30s → auto **HALF_OPEN** (even if you do nothing)  
Heal payment (NONE) + 2–3 orders → CLOSED / CONFIRMED

## If you only see HALF_OPEN
You likely waited > waitDuration after the breaker opened. That means:
OPEN already happened → timer expired → HALF_OPEN.
Re-test: FAIL mode → fire 5 orders quickly → hit circuit-status within a few seconds.
