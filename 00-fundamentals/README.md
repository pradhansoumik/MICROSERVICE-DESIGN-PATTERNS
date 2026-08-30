# 00 — Fundamentals (Interview Concepts)

Theory + interview answers for questions that are **not one pattern demo**.  
Runnable demos live in `01-` … `07-`; this folder **links** to them.

| Doc | Covers |
|---|---|
| **`01-SERVICE-COMMUNICATION.md`** | Sync/async, REST vs Kafka, gateway vs S2S |
| **`02-SECURITY-AUTHN-AUTHZ.md`** | JWT, gateway auth, service-to-service security |
| **`03-DATA-CONSISTENCY.md`** | DB per service, Saga vs 2PC (short) |
| **`04-OBSERVABILITY-ONE-PAGER.md`** | Logs / metrics / traces — pointer to `07-observability` |
| **`rest-clients-demo/`** | Runnable: RestTemplate + RestClient + Feign vs one provider |
**Revise order:** Communication → Security → Consistency → Observability pitch.

**Related pattern demos**

| Topic | Demo |
|---|---|
| Gateway + JWT + rate limit | `03-api-gateway/` |
| Discovery / LB | `05-service-discovery/` |
| Resilience on calls | `01-circuit-breaker/` |
| Async / choreography | `06-event-driven/` (after Kafka) |
| Observability depth | `07-observability/` |
