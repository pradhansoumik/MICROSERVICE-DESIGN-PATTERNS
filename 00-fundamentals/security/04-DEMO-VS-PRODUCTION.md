# 04 — Demo vs Production

| Topic | **This demo** (`security-sso-demo`) | **Production (your company / OCP)** |
|---|---|---|
| IdP | Keycloak in **Docker** (local) | Keycloak / corporate SSO HA |
| Portal | Spring **OAuth2 Client** BFF | Portal / BFF / API Gateway |
| API | One **order-api** resource server | Many services behind Gateway |
| Token validation | JWT via Keycloak **JWKS** | Same idea (issuer-uri / JWKS) |
| Refresh tokens | Handled by Spring Security session/client | Same; store securely |
| UI | Minimal HTML after login | Full SPA / enterprise UI |
| Gateway | Optional (BFF acts as client) | Often **API Gateway** validates + routes |
| S2S | Documented; not full mTLS demo | **Istio mTLS** + policies |
| Secrets | `client-secret` in properties (local only) | Vault / K8s Secrets / sealed secrets |
| HTTPS | Often HTTP localhost | TLS everywhere |
| Roles | Simple realm roles | Fine-grained realm/client roles |

---

## What we intentionally do **not** implement in demo

- Full Istio mTLS mesh  
- HA Keycloak cluster  
- Complex SPA + silent refresh  
- Multi-realm / social login  
- Production secret management  

Those are **replacement notes** for interview talk — see table above.

---

## Other demos in this repo

| Demo | Use when |
|---|---|
| `security-sso-demo` | Real **Keycloak SSO** redirect flow |
| `03-api-gateway/...-auth-ratelimit` | Simple **HS256 JWT** at gateway (no IdP) — learning edge filters |

**Interview:**  
> “Locally I run Keycloak + BFF + resource server for SSO. In production we use corporate Keycloak/SSO, gateway, JWKS validation, and Istio mTLS for service-to-service.”
