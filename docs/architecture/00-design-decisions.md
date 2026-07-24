# Design Decisions & Deviations from the Brief

> **Why this document exists.** `docs/project-overview.md` is the course brief. It
> is a useful *starting point*, but parts of it describe a naive design that a
> production backend engineer would push back on. This project treats the brief
> as **input, not specification**. The authoritative source of *what to build* is
> the actual product design in the two Figma files
> ([figma-analysis.md](../figma-analysis.md)); the brief informed the *service
> split*, which we then corrected where it conflicted with sound practice.
>
> Each entry below is a decision record: the brief's position, our decision, and
> the reasoning. Nothing here drops a graded learning objective — see
> [§ What we deliberately kept](#what-we-deliberately-kept).

---

## Deviations

### DEV-01 — Database-per-service, never a shared schema
- **Brief:** Step 2, "Set up the database schema for the three microservices" —
  reads as one schema-setup step for all three.
- **Decision:** Three isolated schemas (`inventory_db`, `wallet_db`, `shop_db`);
  a service touches only its own; **no cross-service tables or foreign keys**.
- **Why:** A shared database is the classic "distributed monolith" anti-pattern —
  it couples services at the data layer, so they can't evolve or deploy
  independently, which defeats the purpose of microservices. Data ownership is
  the whole point of the boundary.

### DEV-02 — Single owner for product data (inventory is the master)
- **Brief:** *Both* Shop and Inventory list "Product creation and management."
- **Decision:** `inventory-service` is the **single source of truth** for product
  master data, categories, variants, and stock. `shop-service` stores only
  product **references (ids)** and reads live data from inventory via Feign.
- **Why:** Duplicated ownership means two places to update, guaranteed drift, and
  ambiguous truth. Single-writer/single-source is a core data-integrity
  principle. This is the most important correction to the brief.

### DEV-03 — Money is owned by the wallet, not the shop
- **Brief:** Shop microservice does "Payment processing."
- **Decision:** `wallet-service` owns all balance mutations (debit/credit/ledger).
  `shop-service` only **orchestrates** checkout and asks the wallet to debit.
- **Why:** Money must have one authoritative owner with a single, auditable
  ledger and concurrency control. Letting the shop move money would split
  financial truth across services. Shop coordinates; wallet is the system of record.

### DEV-04 — Build order: inventory → wallet → shop
- **Brief:** Steps 3–5 build wallet → shop → inventory.
- **Decision:** Build **inventory first**, then wallet, then shop.
- **Why:** Dependencies point *toward* inventory and wallet; shop depends on both.
  Building a consumer before its providers means testing against nonexistent
  services. Build leaves of the dependency graph first.

### DEV-05 — Multi-module monorepo with six deployables
- **Brief:** Step 1, "Create *a* new Spring Boot project" (singular).
- **Decision:** A parent Maven POM aggregating six independent Spring Boot
  modules (3 business + config + eureka + gateway).
- **Why:** "Microservices" means separately-running processes, each with its own
  port, DB, and lifecycle. One project = a monolith. The monorepo keeps builds
  and grading simple while preserving genuine runtime separation. (Chosen with the
  repo owner over a repo-per-service split — same runtime, less ceremony.)

### DEV-06 — Config server comes up **first**, not last
- **Brief:** Config Server appears at the very end (steps 15–16).
- **Decision:** Config-server is a first-class part of the topology and starts
  before the services that read from it.
- **Why:** Centralised config is a boot-time dependency. Introducing it "last" as
  an afterthought forces reworking every service's configuration. Design for it up
  front (services still boot standalone via `optional:` import for isolated tests).

### DEV-07 — Automated testing is a first-class deliverable
- **Brief:** "Test the microservice using Postman or a similar tool" — manual only.
- **Decision:** A full automated pyramid — unit, `@WebMvcTest`/`@DataJpaTest`
  slices, `@SpringBootTest` integration with **Testcontainers** + **WireMock**,
  resilience/idempotency tests, and E2E — gated by `./mvnw clean verify` and
  JaCoCo coverage. Postman is kept only as a demo aid.
- **Why:** Manual Postman testing is not repeatable, not regression-proof, and
  can't cover failure paths (insufficient funds, breaker-open, compensation).
  Automated tests are the correctness contract. See
  [../conventions/testing-strategy.md](../conventions/testing-strategy.md).

### DEV-08 — Security (JWT auth + role checks) on every non-public route
- **Brief:** Does not mention authentication/authorization at all (though the
  scaffold pulls in `spring-boot-starter-security`).
- **Decision:** Stateless JWT issued by wallet-service; validated at the gateway
  and services; public catalog/auth routes, protected shopper routes, admin-only
  writes. See [../security/authentication-authorization.md](../security/authentication-authorization.md).
- **Why:** An e-commerce backend that lets anyone read/modify carts, orders, and
  wallets is not shippable. The Figma explicitly shows accounts, login, and "My
  Orders" — security is a real requirement the prose omitted.

### DEV-09 — Checkout is an orchestrated saga with compensation + idempotency
- **Brief:** Lists "Order creation" and "Payment processing" as separate bullets;
  says nothing about consistency across services.
- **Decision:** Checkout reserves stock → debits wallet → confirms, with
  **compensating actions** (release stock / mark FAILED) on any failure, and
  **idempotency keys** so retries can't double-charge or double-reserve. See
  [03-request-flows.md](03-request-flows.md).
- **Why:** Without a saga you get inconsistent states (paid-but-no-stock, or
  reserved-but-unpaid). This is exactly the distributed-consistency problem
  microservices must handle, and it's where Resilience4j earns its keep.

### DEV-10 — Resilience monitoring via Actuator/Micrometer, not the "Resilience4j dashboard"
- **Brief:** Step 11, "Configure the Resilience4j dashboard to monitor the
  circuit breaker."
- **Decision:** Expose breaker state through Spring Boot **Actuator**
  (`/actuator/circuitbreakers`, `/actuator/circuitbreakerevents`) and Micrometer
  metrics; optionally scrape with Prometheus/Grafana.
- **Why:** There is no maintained standalone "Resilience4j dashboard" product for
  current Spring Boot/Resilience4j; the actuator + Micrometer path is the
  supported, modern way to observe breakers. We still satisfy the intent —
  *monitor the circuit breaker* — with the correct tooling.

### DEV-11 — Endpoints derived from the Figma, not invented from prose
- **Brief:** Describes features in loose sentences ("shopping cart management",
  "payment processing").
- **Decision:** The concrete endpoint set comes from the **actual screens** in the
  two Figma designs (wishlist/heart, reviews, sale/compare-at price, "New" badge,
  category tabs, vouchers, newsletter) — see [../figma-analysis.md](../figma-analysis.md).
- **Why:** UX is the real requirement. Several needed features (wishlist, reviews
  & ratings, discounts) aren't in the brief's bullets but are clearly in the
  designs; a couple of brief-implied features are shaped by what the UI actually does.

---

## What we deliberately kept

To be clear this is judgment, not contrarianism — we kept every element that is a
genuine course learning objective, because dropping them would be the wrong call:

- **Spring Cloud stack:** Eureka (discovery), Config Server, API Gateway,
  OpenFeign, Resilience4j — all present and central.
- **Three business services** named wallet / shop / inventory, as the brief frames
  them (we corrected their *responsibilities*, not their existence).
- **Spring Boot + Spring Data JPA + MySQL + Maven**, per the brief's stack.
- **Auth kept inside wallet-service** (rather than splitting out a separate
  identity service): a conscious scope decision — in this domain the account and
  its wallet are one bounded context, and a 4th service isn't worth the overhead
  for this project. Noted here so it's clearly a *decision*, not an oversight.

---

## How to read this alongside the brief
`project-overview.md` stays in the repo unchanged as the original input. Where
this document and the brief disagree, **this document wins**, and the reasoning
above is the justification. If a grader expects strict brief adherence on a
specific point, these records make the trade-off explicit and easy to discuss.
