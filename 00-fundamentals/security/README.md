# Security — SSO, OAuth2, JWT (Interview + Demo)

Matches company flow: **open portal → redirect to Keycloak → land on app**.

| Doc | Content |
|---|---|
| **`01-BIG-PICTURE-SSO-KEYCLOAK.md`** | Visual: authn → authz → S2S |
| **`02-TOKENS-JWT-OAUTH2.md`** | Access vs refresh, JWT claims, OAuth2 roles |
| **`03-AUTHZ-AND-S2S.md`** | Roles/scopes, client credentials, mTLS |
| **`04-DEMO-VS-PRODUCTION.md`** | What we run locally vs real OCP |
| **`security-sso-demo/`** | Keycloak + portal-bff + order-api |

Also see short note: `../02-SECURITY-AUTHN-AUTHZ.md`  
Simple HS256 gateway (no Keycloak): `../../03-api-gateway/api-gateway-auth-ratelimit`

**Learn order:** 01 → 02 → 03 → run demo → 04.
