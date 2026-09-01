# DEMO — Keycloak SSO

Needs **Docker** for Keycloak.

## 1) Start Keycloak

```powershell
cd ...\00-fundamentals\security\security-sso-demo
docker compose up -d
```

Wait ~30–60s. Open http://localhost:8180 — Keycloak welcome page.

Realm `interview` should import automatically (user `alice` / `alice123`, client `portal-bff`).

**If realm missing:** Admin console → create realm `interview` → client `portal-bff` (confidential, secret `portal-secret`, redirect `http://localhost:8400/*`) → user `alice` / `alice123` with realm role `USER`.

---

## 2) Start Order API

```powershell
cd ...\security-sso-demo\order-api
mvn spring-boot:run
```

http://localhost:8401/actuator/health → UP

Without token, API should reject:

```powershell
# Expect 401
Invoke-WebRequest http://localhost:8401/api/orders
```

---

## 3) Start Portal BFF

```powershell
cd ...\security-sso-demo\portal-bff
mvn spring-boot:run
```

---

## 4) SSO in browser

1. Open **http://localhost:8400/**  
2. Browser **redirects to Keycloak** (login)  
3. Login: **alice** / **alice123**  
4. Redirect back → portal home (username shown)  
5. Click **Call Order API with access token**  
6. See orders JSON — Order API validated JWT + checked `ROLE_USER`

---

## What you just proved

| Step | Concept |
|---|---|
| Redirect to Keycloak | **Authentication** / SSO |
| Land on portal | OAuth2 client + session |
| `/my-orders` with Bearer | Access token to API |
| Order API accepts | Resource server JWT validation (JWKS) |
| `@PreAuthorize` | **Authorization** by role |

---

## Troubleshooting

| Issue | Fix |
|---|---|
| Redirect URI error | Client redirect must allow `http://localhost:8400/*` |
| 401 from order-api | Keycloak up? `issuer-uri` matches? Token has roles? |
| 403 from order-api | Realm role `USER` not in access token — check protocol mapper / role mapping |
| Realm not imported | Create manually (see step 1) or recreate container |
