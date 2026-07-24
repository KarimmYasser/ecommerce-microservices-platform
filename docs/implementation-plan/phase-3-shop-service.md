# Phase 3 — shop-service

The shopper's world: cart, wishlist, orders, reviews, and the **checkout saga**.
Depends on inventory (Phase 1) and wallet (Phase 2) via Feign.

**Read first:** [domain/shop.md](../domain/shop.md) · [api/shop-api.md](../api/shop-api.md)
· [api/inter-service-feign.md](../api/inter-service-feign.md) ·
[architecture/03-request-flows.md](../architecture/03-request-flows.md).

**Status: done.** Verified 2026-07-24 — 41 tests green across unit, slice (@WebMvcTest, @DataJpaTest), and integration (@SpringBootTest + WireMock).

## Tasks
- [x] Entities: `Cart`/`CartItem`, `WishlistItem`, `Order`/`OrderItem`, `Review`. Snapshots (`unitPriceSnapshot`, `productNameSnapshot`, `authorNameSnapshot`).
- [x] Feign clients: `InventoryClient` (`batch`, `reserve`, `release`), `WalletClient` (`debit`, `credit`, `balance`). `check` was deliberately not wired in — the checkout saga re-fetches live batch pricing and lets `reserve`'s own shortfall response drive the stock decision, so a separate pre-check call would be redundant. No Resilience4j annotations wrap these calls yet; see the Phase 4 note on this.
- [x] Cart endpoints; cart totals computed from **live** inventory prices with fallback.
- [x] Wishlist endpoints.
- [x] **Checkout saga** (`POST /orders`): load cart → reserve stock → debit wallet → confirm; on failure compensate (release / mark FAILED) with idempotency keys = order id. Clear cart on success.
- [x] Order read endpoints + `cancel` (credit refund + release stock).
- [x] Reviews: CRUD operations for product reviews.
- [x] Identity from JWT (`Bearer` token / validated JWT), populated in SecurityContext.
- [x] `@RestControllerAdvice`: downstream-unavailable → 424/503, stock shortfall → 409, payment failed → 402, duplicate -> 409, not found -> 404.

## Tests — 41 total, all green
- [x] **Unit (saga, mocked clients):** happy path call order; reserve-ok + debit-fails → **release called**, order FAILED; reserve-fails → **no debit**, order FAILED; cancel → credit + release. Assert idempotency keys.
- [x] **@WebMvcTest:** cart, wishlist, order, review controllers, validation, auth, status codes (200, 201, 204, 401, 402, 409).
- [x] **@DataJpaTest (Testcontainers MySQL):** cart uniqueness & item persistence, one-review-per-user constraint, order & item query methods.
- [x] **Integration (@SpringBootTest + WireMock stubbing inventory & wallet):**
  - [x] full successful checkout (assert exact outbound requests + headers + keys);
  - [x] insufficient stock (409, no debit);
  - [x] insufficient funds (402, stock released);
  - [x] **idempotency:** retried debit uses `order-100` key.
- [x] `./mvnw -pl shop-service verify` green.

## Done when
Checkout works end-to-end against stubbed dependencies, every failure path leaves
the system consistent (proven by tests), and reviews/wishlist/cart all pass. ✅
