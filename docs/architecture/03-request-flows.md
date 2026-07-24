# Request Flows

Two flows are worth drawing out because they cross service boundaries: **login**
and **checkout**. Everything else is a straightforward single-service CRUD call.

---

## 1. Registration & login (auth)

Auth is owned by `wallet-service`. Registration also creates the user's wallet
in the same transaction.

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as api-gateway
    participant W as wallet-service
    C->>GW: POST /api/v1/auth/register {email,password,name}
    GW->>W: forward
    W->>W: hash password, save User, create Wallet(balance=0)
    W-->>C: 201 Created {userId}
    C->>GW: POST /api/v1/auth/login {email,password}
    GW->>W: forward
    W->>W: verify password, sign JWT(sub=userId, roles)
    W-->>C: 200 {accessToken}
```

The client then sends `Authorization: Bearer <token>` on every subsequent call.
See [../security/authentication-authorization.md](../security/authentication-authorization.md)
for how the gateway and services validate that token.

---

## 2. Checkout (the interesting one)

Checkout spans **three services** and must not leave the system inconsistent
(e.g. stock reserved but wallet not charged). We use a **synchronous
orchestration saga** driven by `shop-service`, with compensating actions on
failure. Every outbound call is wrapped in a Resilience4j circuit breaker.

```mermaid
sequenceDiagram
    participant C as Client
    participant SHOP as shop-service
    participant INV as inventory-service
    participant WAL as wallet-service

    C->>SHOP: POST /api/v1/orders  (checkout my cart)
    SHOP->>SHOP: load cart, compute total, create Order(PENDING)

    SHOP->>INV: POST /inventory/reserve (items)
    alt insufficient stock
        INV-->>SHOP: 409 Conflict
        SHOP->>SHOP: Order -> FAILED (reason: OUT_OF_STOCK)
        SHOP-->>C: 409 with reason
    else reserved
        INV-->>SHOP: 200 reserved

        SHOP->>WAL: POST /wallets/{userId}/debit (amount, orderId)
        alt insufficient funds / wallet down
            WAL-->>SHOP: 402 / 5xx
            SHOP->>INV: POST /inventory/release (compensate)
            SHOP->>SHOP: Order -> FAILED (reason: PAYMENT_FAILED)
            SHOP-->>C: 402 with reason
        else debited
            WAL-->>SHOP: 200 {transactionId}
            SHOP->>SHOP: Order -> CONFIRMED, clear cart
            SHOP-->>C: 201 {order}
        end
    end
```

### Order states
`PENDING → CONFIRMED` (happy path) — or — `PENDING → FAILED` (stock or payment
failure, with any reservation released). `CONFIRMED → CANCELLED` is a later,
user-initiated action that credits the wallet back and releases stock.

### Idempotency
The debit call carries the `orderId` as an idempotency key so a retry (from
Resilience4j) cannot double-charge. See
[../api/inter-service-feign.md](../api/inter-service-feign.md).

### Why synchronous, not event-driven?
The course scope centres on Feign + Resilience4j, not a message broker.
Synchronous orchestration with compensating calls demonstrates the resilience
concepts cleanly. A note in [phase-3](../implementation-plan/phase-3-shop-service.md)
calls out where a message queue would replace this in a production system.
