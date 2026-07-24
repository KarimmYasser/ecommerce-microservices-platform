# API — inventory-service

Base path: `/api/v1` · Reached by clients through the gateway (`:8080`).
Read endpoints are public; write endpoints require `ROLE_ADMIN`.

Conventions for all services: JSON, `snake`-free camelCase fields, RFC-7807-ish
error body (see [../conventions/coding-standards.md](../conventions/coding-standards.md)),
pagination via `?page=&size=&sort=`.

---

## Products (public reads)

| Method | Path | Purpose | Maps to Figma |
|--------|------|---------|---------------|
| GET | `/products` | List/search/filter products (paged) | Home grids, Shop, Best Selling |
| GET | `/products/{id}` | Product detail incl. images + variants | Product detail page |
| GET | `/categories` | List categories | Nav (Catalog/Collection), Best-Selling tabs |
| GET | `/categories/{id}/products` | Products in a category | "Formal Women", "Man/Woman/Boy/Child" |

### `GET /products` query params
| Param | Meaning |
|-------|---------|
| `q` | full-text search on name/description (search icon) |
| `categoryId` | filter by category |
| `isNew` | `true` → New Arrivals |
| `onSale` | `true` → has `compareAtPrice` (Sale) |
| `minPrice` / `maxPrice` | price range |
| `sort` | e.g. `basePrice,asc` · `ratingAverage,desc` · `createdAt,desc` |
| `page` / `size` | pagination |

**200 response (list item shape)**
```json
{
  "content": [{
    "id": 12,
    "name": "Slick formal sneaker shoe",
    "brand": "StepUp",
    "categoryId": 3,
    "basePrice": 2999.00,
    "compareAtPrice": 4999.00,
    "currency": "INR",
    "isNew": true,
    "ratingAverage": 4.5,
    "ratingCount": 20,
    "primaryImageUrl": "https://.../img.jpg"
  }],
  "page": 0, "size": 20, "totalElements": 137, "totalPages": 7
}
```

**`GET /products/{id}` 200** adds `description`, full `images[]`, and
`variants[]` (each with `id, sku, size, color, price, available`).

---

## Admin writes (ROLE_ADMIN)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/products` | Create product (+ images, variants, stock) |
| PUT | `/products/{id}` | Update product |
| DELETE | `/products/{id}` | Soft-delete (`isActive=false`) |
| POST | `/categories` | Create category |
| PUT | `/categories/{id}` | Update category |
| POST | `/products/{id}/variants` | Add a variant |
| POST | `/variants/{variantId}/stock/adjust` | Adjust `quantityOnHand` (+/−) |

---

## Internal (service-to-service, called by shop via Feign)

These are documented in full, with request/response shapes and idempotency, in
[inter-service-feign.md](inter-service-feign.md):

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/inventory/check` | Availability for a list of items (pre-checkout) |
| POST | `/inventory/reserve` | Reserve stock for an order |
| POST | `/inventory/release` | Release a prior reservation (compensation) |
| GET | `/products/batch?ids=` | Fetch many products by id (cart/order rendering) |

> Internal endpoints should be reachable only from inside the mesh. For course
> scope they may share the app but sit under `/inventory/**`; note in Phase 4
> that the gateway does **not** expose `/inventory/**`, `/wallets/*/debit`, etc.

---

## Errors
| Status | When |
|--------|------|
| 400 | validation failure |
| 401/403 | missing/insufficient auth on admin routes |
| 404 | product/category not found |
| 409 | reserve with insufficient available stock |
