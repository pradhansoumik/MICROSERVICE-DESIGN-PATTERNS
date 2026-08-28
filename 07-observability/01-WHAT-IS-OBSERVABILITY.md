# 01 — What is Observability?

**Interview opener:** Monitoring tells you *something is wrong*. Observability helps you **ask new questions** and find *why* — especially across many services.

---

## 1. Simple meaning

| Term | Meaning |
|---|---|
| **Monitoring** | Watch known signals (CPU high, error rate > 5%, “Payment DOWN”) |
| **Observability** | From output (logs/metrics/traces), understand **internal state** of the system — even for failures you didn’t predict |

Microservices make this harder: one user click may touch Gateway → Order → Inventory → Payment.  
You need a way to **follow that path** and see pain points.

---

## 2. The three pillars (must memorize)

```text
                    OBSERVABILITY
           ┌────────────┼────────────┐
           ▼            ▼            ▼
         LOGS        METRICS       TRACES
      (what happened) (how much)  (where across services)
```

| Pillar | Question it answers | Example |
|---|---|---|
| **Logs** | What happened in this service? | `Payment failed: insufficient funds, orderId=O-1` |
| **Metrics** | How is the system doing over time? | `payment_errors_total`, p99 latency, CPU |
| **Traces** | Where did this one request go? | Span: Gateway → Order → Payment (200ms stuck in Payment) |

You need **all three** in production. One alone is not enough.

---

## 3. Easy analogies

| Pillar | Analogy |
|---|---|
| **Logs** | Diary entries (“at 10:01 payment refused card”) |
| **Metrics** | Dashboard gauges (error rate 2%, latency 800ms) |
| **Traces** | Tracking a parcel across warehouses (each hop = span) |

---

## 4. Why interviewers care

Without observability:
- “Order API slow” → which dependency?  
- “5% checkout fail” → Payment or Inventory?  
- Night incident → no story of the request path  

With it:
- Metrics alert → “Payment p99 up”  
- Trace → slow span on Payment  
- Logs → exact exception / business reason  

---

## 5. Where Health fits

**Health** (Actuator `/health`) is often taught with observability:
- Is this instance **alive / ready**?  
- Used heavily by **K8s/OCP probes**  

It’s not a fourth pillar in the classic “3 pillars” pitch, but it’s **day-1 ops** — next concept.

---

## 6. Pitch (20 seconds)

> “Observability for microservices is logs, metrics, and traces. Logs explain events, metrics show trends and alerts, traces show the path of one request across services. Together they turn ‘something’s broken’ into ‘Payment span is slow and here’s the error.’”

---

## Checkpoint

You should be able to answer:
1. Observability vs monitoring?  
2. Name the **three pillars** and one example each.  
3. Why microservices need traces, not only logs?

When ready → concept **2: Health & Actuator**.
