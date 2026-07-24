# Inter-Service Contracts (Feign)

Only **shop-service** calls out to the other two. These endpoints are internal:
the gateway must **not** route public traffic to them (see
[../infrastructure/api-gateway.md](../infrastructure/api-gateway.md)). Every call
is wrapped in a Resilience4j circuit breaker with a defined fallback
([../infrastructure/resilience4j.md](../infrastructure/resilience4j.md)).

Feign clients target services by **Eureka name**, e.g. `lb://inventory-service`.

---

## shop → inventory

### `GET /products/batch?ids=1,2,3`
Enrich cart/order lines with live name/price/image/availability.
→ `[{ id, name, basePrice, currency, primaryImageUrl, active }]`
**Fallback:** use each cart item's `unitPriceSnapshot`/`productNameSnapshot`; flag
lines as "price unconfirmed".

### `POST /inventory/check`
```json
{ "items": [ { "variantId": 34, "quantity": 2 } ] }
```
→ `{ "allAvailable": true, "unavailable": [] }`
Used to show stock status before committing. **No fallback** — treat unknown as
unavailable.

### `POST /inventory/reserve`
Same body as `check`, plus `orderId`. Atomically reserves stock.
→ **200** `{ "reserved": true }` · **409** `{ "reserved": false, "shortfall": [...] }`
**Idempotent** on `orderId`: reserving twice for the same order is a no-op.
**No safe fallback** — if inventory is down, checkout must fail cleanly (do not
sell unknown stock).

### `POST /inventory/release`
`{ "orderId": 123 }` — compensating action; releases a prior reservation.
**Must be retried** if it fails (reservation leak otherwise). Idempotent.

---

## shop → wallet

### `POST /wallets/{userId}/debit`
```json
{ "amount": 5998.00, "currency": "INR", "idempotencyKey": "order-123" }
```
→ **200** `{ "transactionId": 88, "balanceAfter": 4002.00 }`
→ **402** `{ "error": "INSUFFICIENT_FUNDS" }`
**Idempotency is mandatory:** if a transaction with `idempotencyKey` already
exists, return that original result (never double-charge). This is what makes
Resilience4j retries safe.
**Fallback:** none that charges — on breaker-open, fail the checkout with
`PAYMENT_UNAVAILABLE` and release the stock reservation.

### `POST /wallets/{userId}/credit`
`{ "amount", "currency", "idempotencyKey": "refund-order-123" }` — refund on
cancellation. Idempotent; **must be retried** until it succeeds.

### `GET /wallets/{userId}/balance`
→ `{ "balance": 4002.00, "currency": "INR" }`. Optional pre-check before debit.

---

## Cross-cutting rules

| Rule | Why |
|------|-----|
| Propagate the caller's identity via `X-User-Id` header (set by gateway/shop) | Internal calls have no end-user token |
| Every mutating internal call carries an `idempotencyKey` | Retries + at-least-once delivery must be safe |
| Money-moving calls (`debit`/`reserve`) have **no fallback that fabricates success** | Never sell stock you don't have or grant unpaid orders |
| Compensations (`release`, `credit`) are retried until success | Avoid stock/refund leaks |
| Timeouts short (e.g. 2s) + circuit breaker | Prevent cascading failure |

## Contract stability
When you change an internal contract, update **this file first**, then both the
provider controller and the consumer's Feign client in the same commit. Consider
a shared `-contracts` module for the DTOs if drift becomes a problem (note it in
Phase 4 rather than doing it up front).
