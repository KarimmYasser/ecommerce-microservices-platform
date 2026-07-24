# Phase 3 — shop-service

The shopper's world: cart, wishlist, orders, reviews, and the **checkout saga**.
Depends on inventory (Phase 1) and wallet (Phase 2) via Feign.

**Read first:** [domain/shop.md](../domain/shop.md) · [api/shop-api.md](../api/shop-api.md)
· [api/inter-service-feign.md](../api/inter-service-feign.md) ·
[architecture/03-request-flows.md](../architecture/03-request-flows.md).

## Tasks
- [ ] Entities: `Cart`/`CartItem`, `WishlistItem`, `Order`/`OrderItem`, `Review`
      (+ optional `Coupon`). Snapshots (`unitPriceSnapshot`, `productNameSnapshot`,
      `authorNameSnapshot`).
- [ ] Feign clients: `InventoryClient` (`batch`, `check`, `reserve`, `release`),
      `WalletClient` (`debit`, `credit`, `balance`) — each with a fallback and a
      Resilience4j breaker (Phase 4 tunes them; wire the annotations here).
- [ ] Cart endpoints; cart totals computed from **live** inventory prices.
- [ ] Wishlist endpoints.
- [ ] **Checkout saga** (`POST /orders`): load cart → reserve stock → debit wallet
      → confirm; on failure compensate (release / mark FAILED) with idempotency
      keys = order id. Clear cart on success.
- [ ] Order read endpoints + `cancel` (credit refund + release stock, both retried).
- [ ] Reviews: CRUD + Feign call to refresh product rating in inventory.
- [ ] Identity from JWT (`X-User-Id` / validated token), never from body.
- [ ] `@RestControllerAdvice`: downstream-unavailable → 424/503, stock → 409,
      payment → 402.

## Tests (Definition of Done includes these — this is the highest-risk service)
- [ ] **Unit (saga, mocked clients):** happy path call order; reserve-ok +
      debit-fails → **release called**, order FAILED; reserve-fails → **no debit**,
      order FAILED; cancel → credit + release. Assert idempotency keys.
- [ ] **@WebMvcTest:** cart/order/review controllers, validation, auth, status codes.
- [ ] **@DataJpaTest (Testcontainers):** cart uniqueness, one-review-per-user,
      order/item persistence.
- [ ] **Integration (@SpringBootTest + WireMock stubbing inventory & wallet):**
  - [ ] full successful checkout (assert exact outbound requests + headers + keys);
  - [ ] insufficient stock (409, no debit);
  - [ ] insufficient funds (402, stock released);
  - [ ] **resilience:** dependency returns 5xx/timeout → fallback runs, breaker
        opens after threshold, no inconsistent order state;
  - [ ] **idempotency:** a retried debit doesn't double-charge.
- [ ] `./mvnw -pl shop-service verify` green; coverage ≥ threshold.

## Done when
Checkout works end-to-end against stubbed dependencies, every failure path leaves
the system consistent (proven by tests), and reviews/wishlist/cart all pass.
