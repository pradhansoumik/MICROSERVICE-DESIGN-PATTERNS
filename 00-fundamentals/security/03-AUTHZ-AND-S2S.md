# 03 — Authorization & Service-to-Service Security

---

## 1. After authentication — authorization

Token is valid ≠ user may do everything.

```text
JWT valid (authn) OK
        │
        ▼
Check roles / scopes / ownership (authz)
  - has role ORDER_READ?
  - order.customerId == token.sub?
```

| Type | Example |
|---|---|
| **Role-based (RBAC)** | `ADMIN`, `USER` |
| **Scope-based** | `orders.read` |
| **Resource-based** | Only own orders |

Done in **Gateway** (coarse) and/or **each service** (fine).

---

## 2. User-driven call chain

```text
Browser (user Alice)
   → Portal (OAuth2 client, Alice’s access token)
      → Order API  Bearer Alice-token
         → maybe Inventory with same user token
            OR service account (see below)
```

---

## 3. Service-to-Service (no human in the middle)

When Payment calls Fraud-Check **without** a user browser:

| Approach | How |
|---|---|
| **Client credentials** | Service logs into Keycloak as itself → gets access token → calls API |
| **mTLS (Istio)** | Certificates prove `payment-service` identity |
| **Network policies** | Only certain pods can reach Payment |

```text
Payment Service
   │  client_id=payment-service + secret
   ▼
Keycloak token endpoint
   │  access_token (service account)
   ▼
Fraud API  (validates JWT + allows service role)
```

**Your OCP:** Istio **mTLS** often handles “who is calling” at mesh layer; still use tokens for user context when acting on behalf of a user.

---

## 4. Picture — both together

```text
                     ┌─ User SSO (Keycloak login)
                     │
Browser ──► Portal ──┼── Bearer user JWT ──► Order API
                     │
                     └─ Order ──(client credentials / mTLS)──► Payment
```

---

## Interview Q&A

**Q: Authn vs authz?**  
A: Authn = identity (Keycloak). Authz = permissions (roles/scopes/ownership in APIs).

**Q: How do services call each other securely?**  
A: Prefer mTLS on the mesh plus least-privilege network policy; for OAuth2 use client-credentials service accounts. Propagate user JWT when acting for the user.

**Q: Put all authz only at gateway?**  
A: Coarse at gateway is fine; **fine-grained** (resource ownership) belongs in the service that owns the data.
