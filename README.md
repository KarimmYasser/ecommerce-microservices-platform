# E-Commerce Microservices

A Spring Cloud microservices backend for an online clothing/footwear store, built
for the Ejada Microservices final project. Six deployables: three business
services (**wallet**, **inventory**, **shop**) plus **config-server**,
**eureka-server**, and an **api-gateway**.

> **Status:** planning complete, scaffold only. No business code yet — start at
> the [implementation plan](docs/implementation-plan/README.md).

## Documentation
- **Start here:** [CLAUDE.md](CLAUDE.md) — project rules & conventions.
- **Docs index:** [docs/README.md](docs/README.md).
- **Why this design:** [docs/architecture/00-design-decisions.md](docs/architecture/00-design-decisions.md)
  — the requirements come from the Figma designs
  ([docs/figma-analysis.md](docs/figma-analysis.md)), and this design intentionally
  improves on the original brief.
- **Build order:** [docs/implementation-plan/README.md](docs/implementation-plan/README.md).

## Tech stack
Java 17 · Spring Boot 4.1 · Spring Cloud 2025.1 · Spring Data JPA · MySQL · Maven
(multi-module) · Eureka · Config Server · Gateway · OpenFeign · Resilience4j.

## Quick start (once services exist — see Phase 0)
```bash
./mvnw clean verify          # build + run all tests
# then start in order: config-server → eureka-server → services → api-gateway
```
Ports, databases, and required environment variables:
[docs/architecture/02-service-topology.md](docs/architecture/02-service-topology.md).

## Ground rules
No secrets in commits; config via `${ENV}` placeholders only. Testing is a
first-class deliverable (`./mvnw clean verify` must be green). Full rulebook:
[docs/conventions/agent-rules.md](docs/conventions/agent-rules.md).
