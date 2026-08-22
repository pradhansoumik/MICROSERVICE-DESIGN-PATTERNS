# JWT Auth at API Gateway — Interview Notes

**Demo:** `api-gateway-auth-ratelimit` (:8085) + shared order/product backends

---

## 1. Why auth at Gateway?

- Single place to validate identity for external clients  
- Backends can stay on private network  
- Combine with rate limiting by **user** (JWT `sub`)

---

## 2. JWT flow (our demo)

```text
POST /auth/token → JWT (subject=username)
GET  /api/** + Authorization: Bearer <jwt>
Gateway validates signature + expiry → allow or 401
```

Production: token issued by **Auth Server** (Keycloak/Cognito), Gateway validates via JWKS — not a login form on the gateway.

---

## 3. JWT vs API Key (say if asked)

| | JWT | API Key |
|---|---|---|
| Contains claims | Yes (sub, roles, exp) | Usually opaque id |
| Expiry | Built-in | Manual rotation |
| Typical use | User/session APIs | Service/partner APIs |

---

## 4. Auth vs RequestId vs Tracing

| | Purpose |
|---|---|
| JWT | **Who** is calling (authorization/authentication) |
| X-Request-Id | Correlate this request in logs |
| TraceId | Distributed tracing graph |

---

## 5. Pitch

> “I put JWT validation at the API Gateway so every /api call is authenticated before routing. After auth I rate-limit by JWT subject. Token issuance in the demo is a helper endpoint; in production an IdP issues tokens and the gateway only validates them.”
