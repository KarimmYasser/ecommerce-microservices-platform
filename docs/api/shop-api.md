# API — shop-service

Base path: `/api/v1`. All endpoints are authenticated; the user id comes from the
JWT, never the request body.

---

## Cart

| Method | Path | Purpose | Figma |
|--------|------|---------|-------|
| GET | `/cart` | Current user's cart (items enriched from inventory) | Cart icon |
| POST | `/cart/items` | Add an item | "Add to cart" / quick-add arrow |
| PUT | `/cart/items/{itemId}` | Change quantity | cart qty stepper |
| DELETE | `/cart/items/{itemId}` | Remove one line | |
| DELETE | `/cart` | Empty the cart | |

**`POST /cart/items` body** `{ "productId": 12, "variantId": 34, "quantity": 1 }`
The response returns the whole cart with a computed `subtotal`; line details
(name, price, image, in-stock) are fetched from inventory via Feign.

---

## Wishlist (the heart icon)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/wishlist` | List saved products |
| POST | `/wishlist/items` | Add `{ "productId": 12 }` |
| DELETE | `/wishlist/items/{productId}` | Remove |

---

## Orders / checkout

| Method | Path | Purpose | Figma |
|--------|------|---------|-------|
| POST | `/orders` | **Checkout** the cart (runs the saga) | Checkout button |
| GET | `/orders` | Current user's orders (paged) | "My Orders" |
| GET | `/orders/{id}` | Order detail | order detail |
| POST | `/orders/{id}/cancel` | Cancel a CONFIRMED order → refund + release stock | |

**`POST /orders` body** (optional) `{ "couponCode": "WELCOME20" }`
Flow: reserve stock (inventory) → debit wallet → confirm. On any failure the
prior step is compensated and the order is marked `FAILED`. Full sequence:
[../architecture/03-request-flows.md](../architecture/03-request-flows.md).

Responses:
- **201** `{ order }` on success
- **409** `{ "error": "OUT_OF_STOCK", "items": [...] }`
- **402** `{ "error": "PAYMENT_FAILED" }`

---

## Reviews & ratings

| Method | Path | Purpose | Figma |
|--------|------|---------|-------|
| GET | `/products/{productId}/reviews` | Paged reviews for a product | "Customer Review" |
| POST | `/products/{productId}/reviews` | Add `{ rating, title, body }` | leave review |
| DELETE | `/reviews/{id}` | Delete own review | |

Posting/deleting a review triggers a Feign call to inventory to refresh the
product's `ratingAverage`/`ratingCount` (see [../domain/inventory.md](../domain/inventory.md)).

---

## Coupons *(stretch — Modeva "Vouchers and Discounts")*
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/cart/coupon` | Validate & attach `{ "code" }` to cart |
| DELETE | `/cart/coupon` | Remove coupon |

## Newsletter *(stretch — StepUp subscribe)*
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/newsletter/subscribe` | `{ "email" }` |

Mark both blocks clearly as optional; the graded core is cart → checkout → orders
→ reviews.

---

## Errors
| Status | When |
|--------|------|
| 400 | validation |
| 401 | missing/invalid token |
| 402 / 409 | payment / stock failure at checkout |
| 404 | cart item / order / review not found |
| 424 (or 503) | a downstream service (inventory/wallet) unavailable and no fallback possible |
