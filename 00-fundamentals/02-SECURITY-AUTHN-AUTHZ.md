# 02 — Security in Microservices (Authentication & Authorization)

**Common question:** “How do you ensure security — authn / authz?”

---

## 1. Authn vs Authz (say clearly)

| Term | Meaning | Example |
|---|---|---|
| **Authentication** | Who are you? | Login → JWT / session |
| **Authorization** | What may you do? | Role `ADMIN` can cancel any order |

---

## 2. Typical production shape

```text
Client
  │  Authorization: Bearer <JWT>
  ▼
API Gateway
  │  validate JWT (signature, exp, issuer)
  │  optional: check scopes/roles
  │  forward request (+ user claims / same Bearer)
  ▼
Order Service
  │  trust gateway network OR re-validate JWT
  │  authorize: is this user allowed on this order?
  ▼
DB / other services
```

**Pitch:**  
> “We authenticate at the API Gateway with JWT. Downstream services authorize using roles/claims and never expose DB ports publicly. On OpenShift, Istio can add mTLS between services.”

**Demo:**  
- Keycloak SSO: `00-fundamentals/security/security-sso-demo/`  
- Simple HS256 JWT gateway: `03-api-gateway/api-gateway-auth-ratelimit`

**Deep dive (visuals + tokens + S2S + prod notes):** `00-fundamentals/security/`

---

## 3. JWT / OAuth2 (what to know)

| Piece | Role |
|---|---|
| **JWT** | Signed token with claims (`sub`, `roles`, `exp`) |
| **OAuth2 / OIDC** | How tokens are issued (Auth Server / Keycloak / Cognito) |
| **Gateway** | Validate JWT (JWKS in prod; demo may use shared secret) |
| **Refresh tokens** | Long-lived at IdP; access token short-lived |

**Demo vs prod:** Demo may mint JWT on gateway; **prod** IdP issues tokens, gateway only **validates**.

---

## 4. Where to put security checks

| Layer | Responsibility |
|---|---|
| **Gateway** | Authenticate external callers; coarse authz; rate limit; TLS terminate |
| **Each service** | Fine-grained authz (resource ownership); validate token if not in zero-trust mesh-only setup |
| **Mesh (Istio)** | mTLS service-to-service; optional auth policies |
| **Never** | Trust “internal network” alone with no auth in real prod |

---

## 5. Service-to-service security

| Approach | Use |
|---|---|
| Propagate user **JWT** | Downstream acts **as the user** |
| **Client credentials** | Service accounts (batch, internal jobs) |
| **mTLS** | Prove caller is really `payment-service` |
| Network policies | Limit who can talk to whom (OCP/K8s) |

---

## 6. Other must-mention practices

- Secrets in **Vault / K8s Secrets**, not in Git  
- HTTPS / TLS everywhere external  
- Least privilege roles  
- Short-lived tokens  
- Audit logs for auth failures  

---

## 7. Interview Q&A

**Q: Authentication vs authorization?**  
A: Authn = identity. Authz = permissions.

**Q: How do you secure microservices?**  
A: JWT at gateway, authorize in services, TLS/mTLS, secrets management, least privilege. Rate limit at edge.

**Q: Validate JWT only at gateway?**  
A: Common for trusted internal network + gateway as door. Stronger: each service validates or mesh authenticates callers (zero trust).

**Q: How does Payment know the user?**  
A: Gateway forwards `Authorization` Bearer (or user headers set after validation — prefer forwarding the token or signed identity).

**Q: API keys?**  
A: Fine for partner/machine APIs; JWT better for user sessions with claims/expiry.

---

## 8. Tie to your project

| You did / use | Say |
|---|---|
| Gateway JWT demo | Edge authentication |
| OCP + Istio | mTLS / mesh policies |
| OpenSearch | Security/audit log search (ops) |
