# Phase 5 — End-to-End Validation & Hand-off

Prove the whole system works together and package it for demo/grading.

## Tasks
- [ ] **E2E test suite** driving the **gateway** across all services:
      `register → login → browse → add to cart → add to wishlist → checkout →
      assert wallet debited + stock decremented + order CONFIRMED → post review →
      cancel order → assert refund + stock released`.
      Run via `docker-compose` (all six + MySQL) or a scripted multi-service boot.
- [ ] **Failure E2E:** checkout with insufficient funds and with out-of-stock;
      assert clean failures and consistent state.
- [ ] **Postman collection** covering every public endpoint, with an environment
      that captures the JWT from login and reuses it (demo aid, not a replacement
      for automated tests).
- [ ] **Coverage report:** `./mvnw clean verify` green across all modules; publish
      the aggregated JaCoCo report.
- [ ] **Docs/report:** short README run-guide; screenshots of Eureka dashboard,
      a successful checkout, and the circuit-breaker open/close events.
- [ ] Final pass: no secrets in history, no agent attribution, `.gitignore` clean.

## Definition of Done (project complete)
- [ ] `./mvnw clean verify` is green for the whole reactor.
- [ ] The happy-path and failure-path E2E flows pass.
- [ ] The resilience demo (Phase 4) is reproducible and documented.
- [ ] Every checklist in Phases 0–4 is ticked.
- [ ] The repo builds from a fresh clone with only documented env vars.

## Suggested extras (only if time remains)
- Aggregated OpenAPI/Swagger UI per service.
- Distributed tracing (Micrometer Tracing + Zipkin) to visualise the saga.
- Coupons and newsletter (the stretch endpoints) with their own tests.
- CI workflow running `mvn verify` on push.
