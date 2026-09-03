# gRPC — Overview & Flow

**Related:** REST clients → `../rest-clients-demo/` · sync communication → `../01-SERVICE-COMMUNICATION.md`

gRPC is **another sync style** (caller usually waits) — not REST, not Kafka.

---

## 1. What is gRPC?

| | REST (our demos) | gRPC |
|---|---|---|
| Protocol | HTTP + JSON (usually) | HTTP/2 + **Protobuf** |
| Contract | OpenAPI / informal | **`.proto`** file (strong contract) |
| Payload | Text JSON | Binary (smaller/faster) |
| Streaming | Unusual | Built-in (client/server/bidi) |
| Browser | Natural fit | Needs grpc-web / gateway |
| Typical use | Public/external APIs, simple S2S | **Internal** high-performance S2S |

**One-liner:**  
> “**gRPC** (**gRPC Remote Procedure Calls**) is a contract-first, binary RPC framework over HTTP/2. It’s a strong fit for **service-to-service** calls inside your system (Order → Pricing, Inventory → Catalog) where both sides speak gRPC; **REST/JSON** is simpler for public or browser-facing HTTP APIs.”


---

## 2. Flow (unary call — most common)

```text
1) Define contract in product.proto
     service ProductService { rpc GetProduct(Request) returns (Response); }

2) Generate stubs (protoc / Maven plugin)
     → ProductServiceGrpc.ProductServiceImplBase (server)
     → ProductServiceGrpc.ProductServiceBlockingStub (client)

3) Server implements the RPC method
4) Client calls stub.GetProduct(request)  ≈  local method, but network RPC
```

```text
product-grpc-client                         product-grpc-server
       │                                           │
       │  GetProduct(id=P-1)   [HTTP/2 + protobuf] │
       │ ─────────────────────────────────────────►│
       │                                           │ handler runs
       │ ◄─────────────────────────────────────────│
       │  ProductResponse { name, price }          │
```

Same **sync wait** idea as RestTemplate — different wire format and API style.

---

## 3. Where it sits vs REST vs Async

```text
Sync  + human/public HTTP     → REST (RestClient / Feign)
Sync  + internal high volume  → gRPC (optional)
Async + fan-out / saga        → Kafka (async flow note)
```

---

## 4. Demo in this repo

| Path | What |
|---|---|
| `grpc-demo/` | Minimal server + client (see `grpc-demo/README.md`) |

Client exposes a small HTTP endpoint so you can trigger gRPC easily from browser/Postman.

---

## 5. Interview Q&A

**Q: REST vs gRPC?**  
A: REST = JSON/HTTP, easy, browser-friendly. gRPC = protobuf/HTTP/2, faster, strict contracts, better for internal S2S; browsers need extra work.

**Q: Is gRPC async communication?**  
A: The **network call** is usually synchronous from the caller’s POV (wait for response). Async messaging is Kafka/Rabbit. gRPC also supports streams, which are different from message brokers.

**Q: When would you pick gRPC?**  
A: Many internal service calls, strong schema, performance — e.g. Order → Pricing. Keep REST at the edge/gateway for external clients.
