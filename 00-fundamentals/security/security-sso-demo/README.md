# Security SSO Demo — Keycloak + Portal + Order API

Mirrors company flow: **open portal → Keycloak login → app**.

| App | Port | Role |
|---|---|---|
| Keycloak | **8180** | IdP / SSO |
| `portal-bff` | **8400** | OAuth2 **client** (redirect to Keycloak) |
| `order-api` | **8401** | OAuth2 **resource server** (JWT + roles) |

Concepts: `../01-BIG-PICTURE-SSO-KEYCLOAK.md` · `../04-DEMO-VS-PRODUCTION.md`

---

## Docs

| File | Purpose |
|---|---|
| `DEMO.md` | Run Keycloak + apps + login as alice |
| `FLOW.md` | Authn → token → authz path |

---

## Demo user

| User | Password | Role |
|---|---|---|
| `alice` | `alice123` | `USER` |

Keycloak admin: `admin` / `admin` (console at http://localhost:8180)
