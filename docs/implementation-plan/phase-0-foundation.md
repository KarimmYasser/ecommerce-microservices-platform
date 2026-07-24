# Phase 0 — Foundation & Repo Restructure

Turn the single-module Initializr scaffold into the multi-module monorepo and put
the shared build/test tooling in place. **No business logic yet.**

**Status: done.** Verified 2026-07-24 — see the Definition of Done below.

## Tasks
- [x] Convert root `pom.xml` to a **parent** (`<packaging>pom</packaging>`) that
      declares `<modules>` and manages dependency/plugin versions.
- [x] Create six modules, each its own `pom.xml` inheriting the parent:
      `config-server`, `eureka-server`, `api-gateway`, `inventory-service`,
      `wallet-service`, `shop-service`.
- [x] Fix the base package to `com.ejada.ecommerce.<service>` (drop `ecommerece`).
      The old single-module `EcommereceFinalProjectApplication` scaffold was removed.
- [x] Shared test/coverage tooling: `spring-boot-starter-test` (JUnit 5, Mockito,
      AssertJ) and the **JaCoCo** plugin (instrumentation + report) live in the
      **parent's** `<dependencies>`/`<build>` and are inherited by every module.
      **Testcontainers** and **WireMock** are declared directly on the three
      business-service POMs instead (the infra modules — config/eureka/gateway —
      don't need a database or Feign stubs, so they don't carry that weight).
      JaCoCo's `check` goal (hard coverage gate) is deferred to each service's own
      pom once it has real logic to measure, per
      [testing-strategy.md](../conventions/testing-strategy.md).
- [x] Every service's `application.yml` has `spring.application.name`, its port
      ([topology](../architecture/02-service-topology.md)), `config.import`, and
      `eureka.client.service-url` already pointed at config-server/eureka (not just
      a placeholder — Phase 4 is end-to-end *validation* of this wiring under the
      full mesh, not the first time it's written). **Secrets (`DB_PASSWORD`,
      `JWT_SECRET`) are placeholders with no default** — the app refuses to start
      without them supplied via environment.
- [x] Hardened `.gitignore` for `*-local.yml`, `.env`, `*.local`, keystores, plus
      the machine-local `.claude/settings.local.json`.
- [x] Root `README.md` (build/run) and a Postman collection stub at
      `docs/postman/ecommerce-platform.postman_collection.json`.
- [x] `docker-compose.yml` for local MySQL (one instance, three schemas
      pre-created via `docker/mysql-init/01-schemas.sql`). No password is
      committed — `MYSQL_ROOT_PASSWORD` must be set at run time.
- [x] **Beyond the original checklist:** each business service also got
      **Swagger UI** (springdoc-openapi 3.x) with a Bearer-JWT security scheme and
      `persist-authorization: true`, so authorizing once in the UI auto-attaches
      the token to every subsequent call. This required a minimal placeholder
      `SecurityConfig` per service (Swagger/actuator paths public, everything else
      authenticated) — the real JWT filter still lands in Phases 1–3.

## Definition of Done
- [x] `./mvnw clean verify` builds and tests all six modules successfully (exit
      code 0), including real Testcontainers-backed MySQL integration tests for
      the three business services — not just compilation.
- [x] **Live smoke test** (not just unit tests): started `eureka-server` and
      `inventory-service` as separate processes against a docker-compose MySQL,
      confirmed `inventory-service` registered and showed `UP` in Eureka's actual
      registry (`GET /eureka/apps`), then tore both down cleanly. Proves the
      cross-process discovery wiring genuinely works, not just that it compiles.
- [x] JaCoCo, Testcontainers, and WireMock all resolve and run (Testcontainers
      2.x renamed its artifacts to `testcontainers-junit-jupiter` /
      `testcontainers-mysql` — noted here since it's easy to trip over again).
- [x] No secrets in any committed file — verified via `git diff` scan on every
      Phase 0 commit; only clearly-labelled `test-only-secret-...` dummy values
      appear, used solely in `src/test/resources/application.yml`.

## Notable version findings (useful for later phases)
- Spring Cloud Gateway 5.x (matching Spring Cloud 2025.1.2 / Boot 4.1) renamed
  the starter to `spring-cloud-starter-gateway-server-webflux`.
- springdoc-openapi's `3.x` line (not `2.x`) is what targets Spring Boot 4 /
  Spring Framework 7.

## Notes
- Keep the Maven wrapper (`mvnw`) — it's how CI/graders build without a local Maven.
- Don't delete `docs/` or `figma/`.
