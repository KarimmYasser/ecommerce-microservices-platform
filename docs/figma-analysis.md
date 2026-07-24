# Figma Analysis — Requirements Source of Truth

The backend requirements come from the **actual product designs**, not from prose.
Two Figma files were provided; we build the **union** so one backend serves both.
This document records what each screen implies for the API, and is the reference
the endpoint specs in [api/](api/) are derived from.

> Why the Figma and not `project-overview.md`? See
> [architecture/00-design-decisions.md → DEV-11](architecture/00-design-decisions.md#dev-11--endpoints-derived-from-the-figma-not-invented-from-prose).
> The `.fig` files store text per-glyph and don't extract as plain strings, so the
> analysis below is based on rendered screenshots (`figma/*.png`).

---

## Source files
- `figma/Modeva _ clothes ecommerce FE Internship.fig` — a clothing store.
- `figma/StepUp _ Shoes E-Commerce FE Internship.fig` — a footwear store.
- Rendered pages: `figma/modeva-1..3.png`, `figma/stepup-1..3.png`.

---

## Modeva (clothes) — observed UI → backend implication

| Screen element | Backend implication |
|----------------|---------------------|
| Top nav: Catalog, Sale, New Arrival, About | Category list; product filters `onSale`, `isNew` |
| Search icon, account icon, cart icon | Product search; auth/account; cart |
| Hero + promoted product cards ("Product Name in Here", price, "Shop Now") | Featured/product listing endpoint |
| Category tiles: Formal Woman, Casual Style, Formal Men | Categories (gender × style) |
| Product grids with **rating badge (4.95)**, "PRODUCT CATEGORY", name, "$XXX", "See More" | Product list w/ rating, category, price, pagination |
| "Best Outfit for your happiness" curated row | Curated/sorted listing (e.g. by rating) |
| Features strip (Satisfaction, 24/7, Fast Delivery, Secure Payment) | Marketing only — no API |
| **Customer reviews** (name, date, rating, text) | Reviews & ratings endpoints |
| Footer menu: Sale, New Arrivals, Formal/Casual Men/Women | Category/collection filters |
| Footer: FAQ, Customer Service, Refund/Return, Terms, Shipping | Static content — no API (optional CMS) |
| Footer Account: My Account, **My Orders**, **Vouchers and Discounts** | Profile; order history; **coupons** |
| Footer contact: WhatsApp, Email, Address | User profile has phone/contact |

## StepUp (shoes) — observed UI → backend implication

| Screen element | Backend implication |
|----------------|---------------------|
| Top nav: Home, Shop, Collection, Customize | Categories/collections |
| Search, cart, menu icons | Search; cart |
| Hero featured product ("Trendy StepUp Pro", price, "Shop Now") | Featured product |
| Brand logos (ebay/amazon/ajio) | Marketing only — no API |
| "Most Popular Products" / "Trending Shoe" carousel | Sorted listing (popularity) |
| "Best Selling" with tabs **Man / Woman / Boy / Child** | Category filter by audience |
| Product cards: **"New" badge**, **wishlist heart**, name, **price + struck-through original** | `isNew`, wishlist, `basePrice` + `compareAtPrice` |
| Quick-add (arrow) button on card | Add-to-cart |
| **Customer Review** cards (name, stars, text) | Reviews & ratings |
| Footer: **Subscribe to newsletter** | Newsletter subscribe (optional) |
| Footer quick links: Home, Shop, Category, Contact, Privacy | Navigation/static |

---

## Union feature set (what the backend must support)

Derived by combining both designs:

| Feature | Evidence (both unless noted) | Owning service |
|---------|------------------------------|----------------|
| Product catalog w/ images, price | both | inventory |
| **Sale / compare-at price** | StepUp struck-through price; Modeva "Sale" | inventory (`compareAtPrice`) |
| **"New" flag** | StepUp "New" badge; Modeva "New Arrival" | inventory (`isNew`) |
| **Rating (avg + count)** | both (4.95 badge; review stars) | inventory (denormalised) + shop (reviews) |
| Categories (audience/style) | Modeva Formal/Casual×M/W; StepUp Man/Woman/Boy/Child | inventory |
| **Product variants** | shoes = size; clothes = size + colour | inventory (`ProductVariant`) |
| Search | both (search icon) | inventory (`?q=`) |
| **Wishlist / favourites** | both (heart icon) | shop |
| Cart | both (cart icon, quick-add) | shop |
| Checkout / orders | both (cart → buy) | shop (saga) → wallet + inventory |
| **My Orders** history | Modeva footer | shop |
| **Reviews & ratings** | both ("Customer Review") | shop (+ inventory rating refresh) |
| Account / profile / login | Modeva "My Account"; both account icons | wallet (auth) |
| **Vouchers / discounts** | Modeva "Vouchers and Discounts" | shop (`Coupon`) — stretch |
| Newsletter subscribe | StepUp footer | shop — stretch |
| Payment | implied by checkout | wallet (debit) |

## What is explicitly *not* a backend concern
Marketing strips, brand logos, hero imagery, FAQ/Terms/Privacy static pages —
these are front-end/CMS content and do not drive endpoints (a real project might
back FAQ/Terms with a CMS; out of scope here).

## How this maps forward
- Domain models: [domain/inventory.md](domain/inventory.md),
  [domain/wallet.md](domain/wallet.md), [domain/shop.md](domain/shop.md).
- Endpoints: [api/inventory-api.md](api/inventory-api.md),
  [api/wallet-api.md](api/wallet-api.md), [api/shop-api.md](api/shop-api.md).
- Each API doc's "Figma" column traces an endpoint back to the screen above.
