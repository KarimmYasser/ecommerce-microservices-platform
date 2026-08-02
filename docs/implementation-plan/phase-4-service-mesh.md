# Phase 4 — Service Mesh (Config, Discovery, Gateway, Resilience)

Wire the three business services into the Spring Cloud fabric.

**Read first:** [infrastructure/config-server.md](../infrastructure/config-server.md)
· [eureka.md](../infrastructure/eureka.md) · [api-gateway.md](../infrastructure/api-gateway.md)
· [resilience4j.md](../infrastructure/resilience4j.md).

**Status: DONE.** Verified 2026-08-02 — config server, Eureka discovery, API Gateway edge auth filter, and Resilience4j circuit breakers/retries are fully wired, configured, and tested.

## Tasks
- [x] **config-server**: `@EnableConfigServer`, native profile, per-service config files under `config/`. Secrets as `${...}` placeholders only.
- [x] **eureka-server**: `@EnableEurekaServer`; all services register.
- [x] Point each service's `spring.config.import` at config-server and its `eureka.client.service-url` at eureka.
- [x] **api-gateway**: explicit `lb://` routes (public catalog/auth vs protected), JWT auth filter setting `X-User-Id`, internal endpoints **not** exposed.
- [x] **Feign over discovery**: shop's Feign clients resolve by Eureka name (`lb://inventory-service`, `lb://wallet-service`).
- [x] **Resilience4j**: circuit breaker, retry, and time-limiter wrappers (`ResilientInventoryClient`, `ResilientWalletClient`) created and wired with `@CircuitBreaker`, `@Retry`, and `@TimeLimiter`. `OrderServiceImpl`, `CartServiceImpl`, and `WishlistServiceImpl` use resilient clients. Business exception translation (409 -> `InsufficientStockException`, 402 -> `PaymentFailedException`) prevents business errors from tripping circuit breakers.

## Tests / verification (Definition of Done)
- [x] `JwtAuthenticationGatewayFilterTest` 100% green (unit/slice tests covering public path bypass, 401 unauthorized handling, and header mutation).
- [x] Gateway security & route predicates verified by inspection: internal paths (`/inventory/reserve`, `/wallets/*/debit`) don't match any configured route predicate, so with `discovery.locator.enabled: false` they 404 through the gateway.
- [x] Feign calls resolve by discovery name (`inventory-service`, `wallet-service`).
- [x] `./mvnw clean verify` reactor build green.
- [x] No secrets committed anywhere in config files.
- [x] **Resilience demo (simulated outage):** `ResilienceCircuitBreakerTest` validates that circuit breakers transition to `OPEN` state on downstream outage (503), throw `CallNotPermittedException` on subsequent calls, ignore business exceptions (409/402), retry transient failures, and expose metrics via Actuator `/actuator/circuitbreakers` and `/actuator/circuitbreakerevents`.

## Done when
The whole system runs through the gateway with discovery, centralized config, and Resilience4j circuit breaker/retry resilience.
