# gRPC mini-demo

| App | Port | Role |
|---|---|---|
| `product-grpc-server` | **9090** (gRPC) | Implements `ProductGrpcService` |
| `product-grpc-client` | **8302** (HTTP) | Calls server via gRPC; expose REST for easy test |

Concept flow: `../GRPC-OVERVIEW-AND-FLOW.md` (folder: `00-fundamentals/grpc/`)

## Run

```powershell
# Terminal 1
cd ...\grpc-demo\product-grpc-server
mvn spring-boot:run

# Terminal 2
cd ...\grpc-demo\product-grpc-client
mvn spring-boot:run
```

## Test (HTTP → gRPC under the hood)

```powershell
Invoke-RestMethod http://localhost:8302/api/grpc/products
Invoke-RestMethod http://localhost:8302/api/grpc/products/P-1
```

Expect `"calledVia": "gRPC"`.

## Flow

```text
Postman → :8302 /api/grpc/products
              │
              ▼
     product-grpc-client (stub)
              │  gRPC GetProduct / ListProducts
              ▼
     product-grpc-server :9090
```

First build may download `protoc` / plugins (needs network).
