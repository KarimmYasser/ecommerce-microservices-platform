# Phase 4 — Service Mesh (Config, Discovery, Gateway, Resilience)

Wire the three business services into the Spring Cloud fabric. Much of this can be
scaffolded earlier; this phase makes it all work together.

**Read first:** [infrastructure/config-server.md](../infrastructure/config-server.md)
· [eureka.md](../infrastructure/eureka.md) · [api-gateway.md](../infrastructure/api-gateway.md)
· [resilience4j.md](../infrastructure/resilience4j.md).

## Tasks
- [ ] **config-server**: `@EnableConfigServer`, native profile, per-service config
      files under `config/`. Secrets as `${...}` placeholders only.
- [ ] **eureka-server**: `@EnableEurekaServer`; all services register.
- [ ] Point each service's `spring.config.import` at config-server and its
      `eureka.client.service-url` at eureka.
- [ ] **api-gateway**: explicit `lb://` routes (public catalog/auth vs protected),
      JWT auth filter setting `X-User-Id`, internal endpoints **not** exposed.
- [ ] **Feign over discovery**: switch shop's Feign clients to resolve by Eureka
      name (`lb://inventory-service`, `lb://wallet-service`).
- [ ] **Resilience4j**: finalise breaker/retry/time-limiter config per instance
      (`walletDebit`, `inventoryReserve`, `inventoryCheck`, compensations); expose
      actuator `circuitbreakers`/`circuitbreakerevents`.

## Tests / verification (Definition of Done)
- [ ] All six services start in order and appear `UP` in the Eureka dashboard.
- [ ] `curl http://localhost:8888/shop-service/default` returns merged config.
- [ ] Through the **gateway** (`:8080`): public product GET works; protected route
      without token → 401; with token → 200; internal paths (`/inventory/reserve`,
      `/wallets/*/debit`) are **not reachable** from outside.
- [ ] Feign calls resolve by name (no hard-coded URLs) — integration test with the
      real registry, or a `@SpringBootTest` using `spring-cloud-loadbalancer`.
- [ ] **Resilience demo (simulated outage):** stop wallet-service → checkout
      hits fallback, releases reservation, returns `PAYMENT_UNAVAILABLE`; breaker
      opens (visible in `/actuator/circuitbreakerevents`); restart → breaker
      closes → checkout recovers. Capture screenshots for the report.
- [ ] No secrets committed anywhere in the config files.

## Done when
The whole system runs through the gateway with discovery + centralised config, and
the circuit breaker behaves correctly under a simulated outage.
