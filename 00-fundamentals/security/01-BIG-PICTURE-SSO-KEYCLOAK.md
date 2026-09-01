# 01 — Big picture: SSO with Keycloak

**Your company story:** open portal URL → browser redirects to **Keycloak** → login → redirect back → main page.

That is **SSO (Single Sign-On)** using **OAuth2 / OpenID Connect (OIDC)** with Keycloak as the **Identity Provider (IdP)**.

---

## 1. Authn vs Authz (10 seconds)

| | Meaning | In SSO flow |
|---|---|---|
| **Authentication** | Prove who you are | Keycloak login page + password/MFA |
| **Authorization** | What you may do | Roles/scopes in token; checked by Gateway / APIs |

---

## 2. Architecture (easy visual)

```text
┌────────────┐     1. Open /portal          ┌─────────────────┐
│  Browser   │ ───────────────────────────► │  Portal / BFF   │
│            │ ◄── 2. 302 to Keycloak ───── │  (OAuth2 client)│
└─────┬──────┘                              └────────┬────────┘
      │ 3. Login page                                 │
      ▼                                               │
┌─────────────────┐                                   │
│    Keycloak     │  IdP — issues tokens              │
│  (SSO server)   │                                   │
└─────┬───────────┘                                   │
      │ 4. Auth code (redirect back)                  │
      ▼                                               │
┌─────────────────┐  5. Code → tokens                 │
│  Portal / BFF   │ ◄────────────────────────────────┤
│  stores session │  access_token (+ refresh)         │
└────────┬────────┘                                   │
         │ 6. API call with Bearer access_token       │
         ▼                                            │
┌─────────────────┐                                   │
│   Order API     │  Resource Server — validate JWT   │
│  (microservice) │  then AUTHORIZE (roles / owner)   │
└─────────────────┘                                   │
```

**Same idea in production with API Gateway:**

```text
Browser → Gateway/BFF → Keycloak (login)
                ↓
         Bearer JWT → Order / Payment / …
                ↓
         (optional) Istio mTLS between services
```

---

## 3. What happens step-by-step (OIDC Authorization Code)

| Step | Who | What |
|---|---|---|
| 1 | User | Opens portal URL |
| 2 | Portal | Not logged in → **redirect** to Keycloak (`/auth?...`) |
| 3 | User | Enters credentials on Keycloak (SSO) |
| 4 | Keycloak | Redirects back with **authorization code** |
| 5 | Portal | Exchanges code for **access token** (+ **refresh**, **id token**) |
| 6 | Portal | Calls Order API with `Authorization: Bearer <access_token>` |
| 7 | Order API | **Authenticates** token (signature via JWKS, exp, issuer) |
| 8 | Order API | **Authorizes** (role `USER` / owns this order?) |

You do **not** put the Keycloak password into microservices — only tokens.

---

## 4. Where each concern lives

| Concern | Who handles it |
|---|---|
| Show login UI / SSO | **Keycloak** |
| Start login / hold user session | **Portal / BFF / Gateway** (OAuth2 client) |
| Validate JWT on APIs | **Resource servers** (Order, Payment…) or Gateway |
| Fine-grained “can edit order 5?” | **Each microservice** |
| Service proves identity to another service | **Client credentials** and/or **mTLS** (mesh) |

---

## 5. Map to words interviewers use

| Term | In this picture |
|---|---|
| **IdP** | Keycloak |
| **OAuth2 Client** | Portal / BFF |
| **Resource Server** | Order API |
| **Access token** | JWT sent to APIs |
| **Refresh token** | Used by client to get new access token (not sent to APIs usually) |
| **SSO** | One Keycloak login works across apps in the realm |

---

## 6. Pitch (30 seconds)

> “Our portal uses SSO with Keycloak. The browser redirects to Keycloak for authentication; after login the portal receives tokens via the OAuth2 authorization-code flow. APIs are resource servers: they validate the JWT access token and then authorize by roles or resource ownership. Service-to-service calls use client-credentials or mTLS on the mesh—not the user’s password.”
