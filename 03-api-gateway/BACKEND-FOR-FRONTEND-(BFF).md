# Backend for Frontend (BFF)

**Category:** Integration / edge pattern  
**Related:** API Gateway (`README.md`, `FLOW.md`) · Security SSO portal acts like a simple BFF (`00-fundamentals/security/`)

---

## 1. What is BFF?

Backend for Frontend (BFF) is a **variant of the API Gateway** idea with an extra layer between **clients** and **microservices**.

| | API Gateway (typical) | BFF |
|---|---|---|
| Entry points | Often **one** gateway for many clients | **One backend per client type** (or per channel) |
| API shape | Shared / general | **Tailored** to that UI’s needs |
| Goal | Routing, auth, RL, cross-cutting | UX-specific aggregation & shaping |

**One-liner:**  
> “BFF is not one door for everyone — it’s a dedicated backend for each frontend.”

---

## 2. Why use it?

- Avoid one **bloated** API that serves web + mobile + partners poorly  
- **Improve UX** — responses shaped for that client (fields, pagination, payloads)  
- **Performance** — client-specific aggregation; calls to microservices can run in **parallel** behind the BFF  
- Fit **complex UIs** or separate **business channels** without forcing one contract  
- Supports **continuous delivery** at scale when frontends evolve at different speeds  

---

## 3. Example architecture

A company has:

- Web application  
- Mobile application  
- Third-party / partner application  

```text
   Web UI          Mobile App         Partner App
      │                │                   │
      ▼                ▼                   ▼
  Web BFF          Mobile BFF         Partner BFF
      │                │                   │
      └────────┬───────┴────────┬──────────┘
               ▼                ▼
         Order Service    Product Service   …
```

- Each app talks to **its own BFF**  
- Each BFF talks to the **same** (or shared) microservices  
- No single “god API” for all three clients  

---

## 4. BFF vs API Gateway (interview)

| Question | Answer |
|---|---|
| Is BFF a gateway? | It’s a **gateway-style** edge service, specialized **per frontend** |
| Can you use both? | **Yes** — often a shared gateway (TLS, auth) + BFFs, or BFFs alone per channel |
| Main benefit | Client-specific APIs without polluting core microservices |
| Main cost | **More components** to build, deploy, and operate |

---

## 5. When to use / when not to

### Use BFF when
- Web and mobile need **very different** payloads or workflows  
- One shared API is becoming a **bloated** compromise  
- Frontend teams want to move **independently** of core domain APIs  

### Skip (or defer) BFF when
- One simple client / one API is enough  
- Team cannot afford **extra** services and ownership  
- Complexity of design would outweigh UX gains  

> More edge layers ⇒ more setup. Use BFF when client diversity justifies it.

---

## 6. How it fits your demos

| In this repo | Relation to BFF |
|---|---|
| `03-api-gateway` | Classic **single** API Gateway |
| `security-sso-demo/portal-bff` | Small **BFF-like** portal: SSO + calls Order API for the UI |
| Core microservices | Stay domain-focused; BFF adapts for the frontend |

---

## 7. Interview pitch (20 seconds)

> “BFF gives each frontend its own backend API so web and mobile aren’t forced through one bloated contract. The BFF aggregates and shapes calls to microservices. We use it when clients differ a lot; otherwise a single API Gateway may be enough. Trade-off is more services to maintain.”
