# Implementation Plan — Roadmap

Execute phases **in order**. Each phase has a checklist and a **Definition of
Done that includes passing tests** (see
[../conventions/testing-strategy.md](../conventions/testing-strategy.md)). Do not
start a phase until the previous one is green.

## Status board

| Phase | Focus | Status |
|-------|-------|--------|
| 0 | [Foundation & repo restructure](phase-0-foundation.md) | ✅ done |
| 1 | [inventory-service](phase-1-inventory-service.md) (build first — others depend on it) | ✅ done |
| 2 | [wallet-service](phase-2-wallet-service.md) (auth + money) | ✅ done |
| 3 | [shop-service](phase-3-shop-service.md) (cart, orders, checkout saga, reviews) | ✅ done |
| 4 | [Service mesh](phase-4-service-mesh.md) (config, eureka, gateway, feign, resilience) | 🟡 mostly done — resilience4j instances configured but not wired to any call yet |
| 5 | [End-to-end](phase-5-end-to-end.md) (E2E tests, Postman, resilience demo) | 🟡 mostly done — resilience demo still open, see Phase 4 |

> Keep this table current — tick a phase only when its own checklist and
> `./mvnw clean verify` are green.

## Why this order?
- **Inventory first**: it has no outbound dependencies and shop reads from it.
- **Wallet second**: independent (auth + money); shop pays into it.
- **Shop third**: depends on both via Feign — build it once its dependencies exist.
- **Mesh fourth**: config/discovery/gateway/resilience can be layered on
  incrementally, but you can also stand up eureka+config early (Phase 0/1) if you
  prefer running through the gateway from the start. Either is fine; the plan
  writes the business logic first so tests don't wait on infra.

## Cross-cutting expectations for every phase
- Follow [coding-standards.md](../conventions/coding-standards.md).
- Write tests as you go, not after (unit + slice always; integration where it
  crosses a boundary).
- No secrets committed; config via placeholders.
- Update the relevant `docs/` file if a contract changes.
- Commit in small Conventional-Commit steps on a feature branch.

## Suggested parallelisation
Phases 1 and 2 are independent and could be built in parallel by two people. Phase
3 needs 1 & 2. Phase 4 needs 1–3. Phase 5 needs everything.
