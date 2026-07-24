# Phase 1 — inventory-service

Build the product catalog: the foundation everything else reads from.

**Read first:** [domain/inventory.md](../domain/inventory.md) ·
[api/inventory-api.md](../api/inventory-api.md).

## Tasks
- [ ] Entities: `Category`, `Product`, `ProductImage`, `ProductVariant`,
      `StockItem` with JPA mapping, auditing, and constraints (unique slug/sku).
- [ ] Repositories (Spring Data) with query methods for search/filter/paging.
- [ ] Service layer:
  - [ ] Public reads: list/search/filter products, get detail, list categories,
        products-by-category, `products/batch`.
  - [ ] Admin writes: CRUD product/category/variant, stock adjust.
  - [ ] Reservation logic: `check`, `reserve`, `release` with the
        available = onHand − reserved invariant (atomic, guarded by `@Version` or
        pessimistic lock).
- [ ] Controllers for the [inventory API](../api/inventory-api.md); DTOs (records).
- [ ] Security: public GETs; writes require `ROLE_ADMIN` (JWT validation filter,
      see [security](../security/authentication-authorization.md)).
- [ ] Seed data (categories + sample products/variants/stock) via
      `CommandLineRunner`/`data.sql` so the catalog is non-empty for demos.
- [ ] `@RestControllerAdvice` mapping not-found→404, insufficient-stock→409.

## Tests (Definition of Done includes these)
- [ ] **Unit:** reservation math (reserve success/insufficient/release,
      idempotent reserve by orderId); search/filter service logic; mappers.
- [ ] **@DataJpaTest (Testcontainers MySQL):** custom queries, unique
      constraints, pagination.
- [ ] **@WebMvcTest:** filter params → correct query; admin route without
      ADMIN → 403; validation → 400; reserve insufficient → 409.
- [ ] **@SpringBootTest integration:** full product create→list→reserve→release
      cycle against real DB.
- [ ] `./mvnw -pl inventory-service verify` green; coverage ≥ threshold.

## Done when
Catalog browsing + reservation endpoints work end-to-end against a real DB, all
tests green, and shop-service can later call `batch`/`reserve`/`release`.
