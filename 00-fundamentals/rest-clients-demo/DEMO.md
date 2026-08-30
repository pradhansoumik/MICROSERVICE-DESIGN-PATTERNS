# DEMO — REST clients (one by one)

## Start (2 terminals)

```powershell
cd ...\00-fundamentals\rest-clients-demo\product-provider
mvn spring-boot:run

cd ...\00-fundamentals\rest-clients-demo\product-caller
mvn spring-boot:run
```

Check provider: http://localhost:8301/products

---

## 1) RestTemplate

```powershell
Invoke-RestMethod http://localhost:8300/api/rest-template/products
Invoke-RestMethod http://localhost:8300/api/rest-template/products/P-1
```

Expect: `"calledVia": "RestTemplate"` and product data from provider.

**Code:** `CallerController.viaRestTemplate` + `HttpClientConfig.restTemplate`

---

## 2) RestClient

```powershell
Invoke-RestMethod http://localhost:8300/api/rest-client/products
Invoke-RestMethod http://localhost:8300/api/rest-client/products/P-1
```

Expect: `"calledVia": "RestClient"`

**Code:** `CallerController.viaRestClient` + `HttpClientConfig.restClient`

---

## 3) OpenFeign

```powershell
Invoke-RestMethod http://localhost:8300/api/feign/products
Invoke-RestMethod http://localhost:8300/api/feign/products/P-1
```

Expect: `"calledVia": "OpenFeign"`

**Code:** `ProductFeignClient` + `CallerController.viaFeign`  
Note: `@EnableFeignClients` on the application class; `url=${provider.base-url}` (no Eureka in this demo).

---

## Compare

| Endpoint prefix | Client | Style |
|---|---|---|
| `/api/rest-template/**` | RestTemplate | Imperative sync |
| `/api/rest-client/**` | RestClient | Fluent sync |
| `/api/feign/**` | `@FeignClient` | Declarative interface |
