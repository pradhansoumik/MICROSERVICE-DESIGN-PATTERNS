# Demo — JWT Auth + Rate Limiting Gateway (:8085)

Uses **common** backends: `order-backend` :8101, `product-backend` :8102  
Secured gateway: **`api-gateway-auth-ratelimit` :8085**  
(Basic gateway :8080 can stay stopped.)

## Start
```powershell
# Terminal 1–2 (shared backends)
cd ...\03-api-gateway\order-backend
mvn spring-boot:run

cd ...\03-api-gateway\product-backend
mvn spring-boot:run

# Terminal 3 (secured gateway)
cd ...\03-api-gateway\api-gateway-auth-ratelimit
mvn spring-boot:run
```

## Step 1 — No token → 401
```powershell
Invoke-RestMethod http://localhost:8085/api/products
```
Expect error / 401 JSON: `UNAUTHORIZED`

## Step 2 — Get JWT
```powershell
$tokenResponse = Invoke-RestMethod -Method POST -Uri http://localhost:8085/auth/token `
  -ContentType "application/json" `
  -Body '{"username":"alice"}'
$token = $tokenResponse.accessToken
$token
```

## Step 3 — Call with JWT → 200
```powershell
Invoke-RestMethod http://localhost:8085/api/products `
  -Headers @{ Authorization = "Bearer $token" }
```

## Step 4 — Rate limit → 429
Limit is **5 requests / 60 seconds** per JWT user (`alice`).

```powershell
1..7 | ForEach-Object {
  try {
    Invoke-WebRequest http://localhost:8085/api/products -Headers @{ Authorization = "Bearer $token" } |
      Select-Object StatusCode
  } catch {
    $_.Exception.Response.StatusCode.value__
  }
}
```
Expect first ~5 succeed (200), then **429** with `RATE_LIMIT_EXCEEDED`.

## Step 5 — Different user = separate bucket
```powershell
$tokenBob = (Invoke-RestMethod -Method POST -Uri http://localhost:8085/auth/token `
  -ContentType "application/json" -Body '{"username":"bob"}').accessToken

Invoke-RestMethod http://localhost:8085/api/products -Headers @{ Authorization = "Bearer $tokenBob" }
```
Bob has his own limit counter.

## Interview talking points while demoing
1. Auth at edge (JWT) before backends  
2. Rate limit keyed by JWT `sub` (user)  
3. Demo = in-memory fixed window; production design = Redis + token bucket (see `DESIGN-RATE-LIMITING-SYSTEM.md`)
