# System Overview

## Purpose

A backend for an online clothing/footwear store. It is intentionally built as
**microservices** to satisfy the course objectives: service discovery, an API
gateway, externalised configuration, inter-service calls, and resilience.

## The six deployables

| # | Module | Type | Responsibility |
|---|--------|------|----------------|
| 1 | `config-server`   | Infra | Serves centralised configuration to all services. |
| 2 | `eureka-server`   | Infra | Service registry / discovery. |
| 3 | `api-gateway`     | Infra | Single entry point; routing, load-balancing, JWT gate. |
| 4 | `inventory-service` | Business | Products, categories, variants, stock. |
| 5 | `wallet-service`  | Business | Users, auth, wallet, transactions, payments. |
| 6 | `shop-service`    | Business | Cart, wishlist, orders, reviews, checkout orchestration. |

### Why these boundaries?

- **A service owns its data.** No service reads another service's database. All
  cross-service data access goes through REST/Feign.
- **Product master lives in `inventory-service`**, not shop. The shop shows a
  catalog by *reading* products from inventory (via Feign) and layers on
  shopping concepts (cart/order/review). This keeps "what exists & how many are
  in stock" separate from "what a shopper is doing".
- **Users & money live in `wallet-service`.** Authentication is here because the
  user identity and their wallet are the same bounded context. Orders are paid
  by debiting the wallet through a Feign call.

## Technology stack (fixed versions)

| Concern | Choice |
|---------|--------|
| Language | Java 17 |
| Framework | Spring Boot 4.1.x |
| Cloud | Spring Cloud 2025.1.x |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8 (one schema per service) |
| Security | Spring Security + JWT (jjwt) |
| Validation | Jakarta Bean Validation |
| Inter-service | Spring Cloud OpenFeign |
| Discovery | Netflix Eureka |
| Config | Spring Cloud Config |
| Gateway | Spring Cloud Gateway |
| Resilience | Resilience4j (circuit breaker, retry, time limiter) |
| Boilerplate | Lombok |
| Build | Maven (multi-module) |

> These are already declared in the root `pom.xml`. Do not add competing
> libraries (e.g. a second JSON mapper or a different HTTP client) without
> justification.

## Repository layout (target)

The repo is currently a **single-module** Spring Initializr scaffold. Phase 0
restructures it into a multi-module monorepo:

```
ejada-final-project/
├── pom.xml                 # parent (packaging=pom) — dependency & version management
├── config-server/
├── eureka-server/
├── api-gateway/
├── inventory-service/
├── wallet-service/
├── shop-service/
├── docs/
└── figma/
```

Each module has its own `pom.xml` (inheriting from the parent), its own
`src/main/java`, and its own `application.yml`.

Base package convention: `com.ejada.ecommerce.<service>` — e.g.
`com.ejada.ecommerce.inventory`. (The Initializr scaffold used the misspelled
`ecommerece`; correct it to `ecommerce` during Phase 0.)

## Related reading
- Ports, DBs, startup order → [02-service-topology.md](02-service-topology.md)
- Auth & checkout sequences → [03-request-flows.md](03-request-flows.md)
- Where features came from → [../figma-analysis.md](../figma-analysis.md)
