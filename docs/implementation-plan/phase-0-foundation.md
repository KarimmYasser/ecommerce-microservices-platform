# Phase 0 — Foundation & Repo Restructure

Turn the single-module Initializr scaffold into the multi-module monorepo and put
the shared build/test tooling in place. **No business logic yet.**

## Current state
- One module: `com.ejada.ecommerece_final_project` (note the misspelling).
- `pom.xml` already has all needed dependencies (web, jpa, security, validation,
  cloud config/eureka/openfeign/resilience4j, mysql, lombok, test starters).

## Tasks
- [ ] Convert root `pom.xml` to a **parent** (`<packaging>pom</packaging>`) that
      declares `<modules>` and manages dependency/plugin versions. Move the app
      dependencies down into the individual modules that need them.
- [ ] Create six modules, each its own `pom.xml` inheriting the parent:
      `config-server`, `eureka-server`, `api-gateway`, `inventory-service`,
      `wallet-service`, `shop-service`.
- [ ] Fix the base package to `com.ejada.ecommerce.<service>` (drop `ecommerece`).
      Remove/replace the old `EcommereceFinalProjectApplication`.
- [ ] Add to the **parent** `pom.xml` for all modules to inherit:
      **Testcontainers** (mysql, junit-jupiter), **WireMock**, **AssertJ**
      (via boot), and the **JaCoCo** plugin with a coverage check.
- [ ] Give each service an `application.yml` with `spring.application.name`, its
      port ([topology](../architecture/02-service-topology.md)), and
      `config.import` placeholder. **Secrets as `${...}` placeholders only.**
- [ ] Harden `.gitignore` for `*-local.yml`, `.env`, `*.local`, keystores.
- [ ] Add a root `README.md` (how to build/run) and a Postman collection stub.
- [ ] (Optional) `docker-compose.yml` for MySQL to make local runs one command.

## Definition of Done
- [ ] `./mvnw clean install` builds all six (empty) modules successfully.
- [ ] Each service starts (even with no endpoints) and, if eureka is up,
      registers.
- [ ] JaCoCo and Testcontainers/WireMock resolve (a trivial sample test runs).
- [ ] No secrets in any committed file (`git diff --cached` clean).

## Notes
- Keep the Maven wrapper (`mvnw`) — it's how CI/graders build without a local Maven.
- Don't delete `docs/` or `figma/`.
