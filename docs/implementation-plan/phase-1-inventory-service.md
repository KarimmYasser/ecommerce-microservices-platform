# Phase 1 — inventory-service

Build the product catalog: the foundation everything else reads from.

**Read first:** [domain/inventory.md](../domain/inventory.md) ·
[api/inventory-api.md](../api/inventory-api.md).

**Status: done.** Verified 2026-07-24 — 72 tests green, full 6-module reactor
build unaffected. See the Definition of Done below.

## Tasks
- [x] Entities: `Category`, `Product`, `ProductImage`, `ProductVariant`,
      `StockItem` with JPA mapping, auditing, and constraints (unique slug/sku).
      Also added `InventoryReservation`/`InventoryReservationItem` (not in the
      original domain doc) — a small ledger keyed by `orderId` that's what
      actually makes reserve/release idempotent; see the domain doc's stock
      reservation semantics.
- [x] Repositories (Spring Data) with query methods for search/filter/paging,
      via composable `ProductSpecifications` (q, category, isNew, onSale,
      price range) rather than one large custom query.
- [x] Service layer:
  - [x] Public reads: list/search/filter products, get detail, list categories,
        products-by-category, `products/batch`.
  - [x] Admin writes: CRUD product/category/variant, stock adjust.
  - [x] Reservation logic: `check`, `reserve`, `release` with the
        available = onHand − reserved invariant. Implemented with a
        **pessimistic write lock** (`StockItemRepository.findByVariantIdForUpdate`)
        rather than optimistic `@Version` retry, locking every involved
        `StockItem` — in a stable variantId order, to avoid cross-request
        deadlocks — before mutating any of them, so a short item never leaves
        siblings partially reserved.
- [x] Controllers for the [inventory API](../api/inventory-api.md); DTOs (records).
- [x] Security: public GETs; writes require `ROLE_ADMIN`. Implemented the real
      JWT validation filter here (not deferred) — see
      [security](../security/authentication-authorization.md) and
      `config/JwtService.java` / `JwtAuthenticationFilter.java`.
- [x] Seed data (categories + sample products/variants/stock) via
      `CommandLineRunner` (`DataSeeder`, `@Profile("!test")`), drawn from both
      source Figma designs.
- [x] `@RestControllerAdvice` mapping not-found→404, duplicate slug/sku→409,
      invalid stock adjustment→400. Reserve's 409 shortfall body is a domain
      *result*, not routed through the advice — its response shape (`reserved`,
      `shortfall`) differs from the standard error envelope by design; see
      [inter-service-feign.md](../api/inter-service-feign.md).

## Tests — 72 total, all green
- [x] **Unit (43):** `StockServiceImplTest` (13 — check/reserve/release
      idempotency, "no partial reservation on a short sibling item",
      concurrency-adjacent adjustStock guards), `ProductServiceImplTest` (9),
      `CategoryServiceImplTest` (5), mapper tests (6).
- [x] **@DataJpaTest (11, Testcontainers MySQL):** `ProductRepositoryTest` (8 —
      every filter dimension + pagination), `CategoryRepositoryTest` (3 —
      unique slug constraint).
- [x] **@WebMvcTest (23):** all four controllers — public reads, 401 (no
      token) vs 403 (wrong role) vs 200/201/400/404/409, validation failures.
- [x] **@SpringBootTest integration (4, live embedded server + Testcontainers):**
      full create→list→reserve→(idempotent replay)→release→(idempotent replay)
      cycle over real HTTP; 409-with-no-mutation on insufficient stock; a
      **10-thread concurrent-reserve test** against 5 units of stock, asserting
      exactly 5 succeed and final availability is exactly zero — proves the
      pessimistic lock actually serializes concurrent reservations, not just
      that the code compiles; and the admin-role rejection path (401 vs 403).
- [x] `./mvnw -pl inventory-service verify` green. Also verified the **full
      6-module `./mvnw clean verify`** stays green (inventory-service's new
      dependencies/security didn't regress the other five modules).

## A real bug this caught (worth knowing about)
The integration test's "wrong role → 403" case initially got 401 instead —
only reproduced against the live embedded server, never in `@WebMvcTest`.
Root cause: Tomcat error-dispatches to `/error` before returning an
`AccessDeniedException`'s 403 to the client; `/error` wasn't in the security
config's public paths, so that second pass through the same filter chain hit
the custom `AuthenticationEntryPoint` and clobbered the original 403 with 401.
Fixed by adding `/error` to the public path list — see the commit for
`SecurityConfig`. This is exactly the kind of bug a slice test structurally
cannot catch; keep the full-server integration test even once shop-service
adds WireMock-stubbed variants.

## Notable version findings (useful for later phases)
- Several Boot 4.1 test-support classes moved packages vs. Boot 3.x:
  `@DataJpaTest`/`@AutoConfigureTestDatabase` → `org.springframework.boot.data.jpa.test.autoconfigure` /
  `org.springframework.boot.jdbc.test.autoconfigure`; `@WebMvcTest` →
  `org.springframework.boot.webmvc.test.autoconfigure`; `TestRestTemplate` →
  `org.springframework.boot.resttestclient` (needs the
  `spring-boot-resttestclient` + `spring-boot-restclient` artifacts explicitly
  — not pulled in by the other test starters); `@MockBean` is gone, use
  `@MockitoBean` from `org.springframework.test.context.bean.override.mockito`.
- `@WebMvcTest`'s component scan auto-includes `Filter`-typed beans (so a
  custom `OncePerRequestFilter` like `JwtAuthenticationFilter` shows up for
  free) but does **not** auto-include a plain `@Configuration` class like
  `SecurityConfig` — without an explicit `@Import(SecurityConfig.class)`, Boot
  silently falls back to its default auto-configured security (CSRF enabled,
  everything requires auth), which reads as "every test gets 401/403" and is
  easy to misdiagnose as a JWT bug.

## Done when
Catalog browsing + reservation endpoints work end-to-end against a real DB, all
tests green, and shop-service can later call `batch`/`reserve`/`release`. ✅
