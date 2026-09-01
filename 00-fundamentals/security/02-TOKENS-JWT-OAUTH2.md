# 02 — Tokens: JWT, OAuth2, Access vs Refresh

---

## 1. OAuth2 vs OIDC vs JWT

| Term | Meaning |
|---|---|
| **OAuth2** | Framework for **authorization** / delegated access (tokens) |
| **OIDC** | Identity layer on OAuth2 (login, **id_token**, user info) — what SSO uses |
| **JWT** | Token **format** (header.payload.signature) — often used as access token |

Keycloak does OIDC; access tokens are often JWTs.

---

## 2. Access token vs Refresh token

| | **Access token** | **Refresh token** |
|---|---|---|
| Purpose | Call APIs (`Authorization: Bearer …`) | Get a **new** access token when old one expires |
| Lifetime | Short (e.g. 5–15 min) | Longer |
| Sent to microservices? | **Yes** | **No** (only to Keycloak token endpoint) |
| If stolen | Limited window | More dangerous — protect carefully |

```text
Login once
   │
   ├─► access_token   ──► Order API, Payment API, …
   └─► refresh_token  ──► Keycloak only (renew access)
```

---

## 3. What’s inside a JWT (claims)

```text
header.payload.signature
```

Typical claims:

| Claim | Meaning |
|---|---|
| `sub` | User id |
| `iss` | Issuer (Keycloak realm URL) |
| `exp` | Expiry |
| `aud` / `azp` | Audience / authorized party |
| `realm_access.roles` | Roles (Keycloak style) |
| `scope` | Scopes (e.g. `openid profile`) |

**Resource server** checks: signature (JWKS), `iss`, `exp`, then roles.

---

## 4. How APIs validate (no shared secret in prod)

```text
Order API
  │
  │  GET https://keycloak/.../protocol/openid-connect/certs  (JWKS)
  │  verify JWT signature + exp + issuer
  ▼
OK → continue to authorization
```

Demo HS256 gateway (shared secret) ≠ production JWKS/RS256 from Keycloak.

---

## 5. ID token vs access token

| Token | For |
|---|---|
| **ID token** | Portal knows **who** logged in (OIDC) |
| **Access token** | APIs authorize **API calls** |

Don’t send id_token as Bearer to APIs unless designed that way — use **access token**.

---

## Interview one-liners

> “Access token is for APIs; refresh token renews access at the IdP and stays with the client.”  
> “We validate JWT with Keycloak JWKS — signature, issuer, expiry — then check roles.”
