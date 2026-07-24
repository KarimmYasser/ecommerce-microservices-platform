# Phase 4 — Service Mesh (Config, Discovery, Gateway, Resilience)

Wire the three business services into the Spring Cloud fabric.

**Read first:** [infrastructure/config-server.md](../infrastructure/config-server.md)
· [eureka.md](../infrastructure/eureka.md) · [api-gateway.md](../infrastructure/api-gateway.md)
· [resilience4j.md](../infrastructure/resilience4j.md).

**Status: mostly done.** Verified 2026-07-24 — config server, Eureka discovery, and the API Gateway edge auth filter are wired and tested. Resilience4j circuit breaker/retry/time-limiter **instances are configured** (and visible via `/actuator/circuitbreakers`) but are **not yet wrapped around any Feign call** with `@CircuitBreaker`/`@Retry`/`@TimeLimiter` — see the note below.

## Tasks
- [x] **config-server**: `@EnableConfigServer`, native profile, per-service config files under `config/`. Secrets as `${...}` placeholders only.
- [x] **eureka-server**: `@EnableEurekaServer`; all services register.
- [x] Point each service's `spring.config.import` at config-server and its `eureka.client.service-url` at eureka.
- [x] **api-gateway**: explicit `lb://` routes (public catalog/auth vs protected), JWT auth filter setting `X-User-Id`, internal endpoints **not** exposed.
- [x] **Feign over discovery**: shop's Feign clients resolve by Eureka name (`lb://inventory-service`, `lb://wallet-service`).
- [ ] **Resilience4j**: circuit breaker, retry, and time-limiter instances are defined (`walletDebit`, `inventoryReserve`, `inventoryCheck`, `inventoryRelease`, `walletCredit`) and exposed via Actuator, but no `@CircuitBreaker`/`@Retry`/`@TimeLimiter` annotation in shop-service actually uses them yet. Today, resilience is handled manually in `OrderServiceImpl`: it catches `FeignException`, inspects the HTTP status to distinguish a business failure (409 stock shortfall, 402 insufficient funds) from a downstream outage, and runs the matching compensation (release stock / mark order FAILED). This gives the same user-facing behavior as a circuit breaker's fallback for a single call, but without fail-fast-after-repeated-failures or automatic recovery — the breakers currently sit in the registry unused. Wiring them in requires moving the Feign calls behind a separate bean (annotations don't apply to self-invoked calls within the same class, the same Spring AOP proxy limitation documented in Phase 1/2 for locking) — left as a follow-up, not attempted here to avoid destabilizing the saga's compensation logic under time pressure.

## Tests / verification (Definition of Done)
- [x] `JwtAuthenticationGatewayFilterTest` 100% green (unit/slice tests covering public path bypass, 401 unauthorized handling, and header mutation).
- [x] Gateway security & route predicates verified by inspection: internal paths (`/inventory/reserve`, `/wallets/*/debit`) don't match any configured route predicate, so with `discovery.locator.enabled: false` they 404 through the gateway. Not exercised by a running multi-service test.
- [x] Feign calls resolve by discovery name (`inventory-service`, `wallet-service`).
- [x] `./mvnw clean verify` reactor build green.
- [x] No secrets committed anywhere in config files.
- [ ] **Resilience demo (simulated outage):** not done — no test stops a dependency and asserts the breaker opens, since nothing is wired to a breaker yet. This was silently dropped from the checklist in an earlier draft of this doc instead of being marked incomplete; restored here as an open item.

## Done when
The whole system runs through the gateway with discovery + centralised config. The
circuit breaker behaving correctly under a simulated outage is **not yet true** —
see the Resilience4j note above.
