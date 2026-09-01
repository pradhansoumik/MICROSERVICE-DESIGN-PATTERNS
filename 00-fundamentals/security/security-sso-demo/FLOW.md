# FLOW — SSO authn → token → authz

```text
Browser
  │  GET http://localhost:8400/
  ▼
portal-bff (OAuth2 Client)
  │  not logged in
  │  302 → Keycloak login
  ▼
Keycloak :8180  (AUTHENTICATION)
  │  alice / alice123
  │  302 back with authorization code
  ▼
portal-bff
  │  code → access_token (+ refresh, id_token)
  │  show home page
  │
  │  GET /my-orders
  │  Authorization: Bearer <access_token>
  ▼
order-api (Resource Server)
  │  1) Validate JWT via Keycloak JWKS  ← authentication of the call
  │  2) @PreAuthorize hasRole USER      ← authorization
  │  3) return orders JSON
  ▼
portal-bff → HTML page with orders
```

## Who is who

| Component | OAuth2 role |
|---|---|
| Keycloak | Identity Provider (IdP) |
| portal-bff | OAuth2 **Client** |
| order-api | OAuth2 **Resource Server** |
| access_token | JWT sent to APIs |
| refresh_token | Stays with client / Spring — renew access at Keycloak |

## Production replacement

Same flow with corporate Keycloak + API Gateway + Istio mTLS — see `../04-DEMO-VS-PRODUCTION.md`.
