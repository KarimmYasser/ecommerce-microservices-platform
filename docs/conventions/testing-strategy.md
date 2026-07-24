# Testing Strategy

> **Testing is a primary deliverable, weighted as highly as the features
> themselves.** A feature is *not done* until it is covered by passing tests. No
> phase is complete while any test is red. Write as many tests as the behaviour
> demands — thoroughness is the goal, not a minimum count.

This project already ships the Spring Boot test starters for each concern
(`*-test`), so the tooling is in place from day one.

---

## 1. The testing pyramid (what to write, and how much)

```
        /\        E2E across the running mesh (few, high value)
       /  \       — full checkout through the gateway
      /----\      Integration tests (many)
     /      \     — @SpringBootTest + Testcontainers MySQL, real repos,
    /        \      WireMock for Feign, security filter, resilience
   /----------\   Slice tests (many)
  /            \  — @WebMvcTest (controllers), @DataJpaTest (repos)
 /--------------\ Unit tests (the most — fast, no Spring context)
                  — services, mappers, domain rules, saga logic
```

Aim wide at the bottom (fast unit tests for every branch of business logic) and
keep a smaller set of high-value integration/E2E tests. **Every bug fixed gets a
regression test that fails before the fix and passes after.**

---

## 2. Layer-by-layer

### 2.1 Unit tests (JUnit 5 + Mockito) — no Spring context
Test one class in isolation; mock its collaborators.
- **Services:** every branch. e.g. wallet debit: success, insufficient funds,
  idempotent replay (same key returns prior result), concurrent version conflict.
- **Domain rules:** balance never < 0; available = onHand − reserved; order state
  transitions (PENDING→CONFIRMED/FAILED/CANCELLED) and illegal transitions rejected.
- **Mappers:** entity ↔ DTO round-trips, null handling.
- **Saga orchestration (shop):** with mocked Feign clients, assert the exact call
  sequence and compensation — reserve→debit→confirm; reserve→debit fails→**release
  called**→order FAILED; reserve fails→no debit, order FAILED.

### 2.2 Web slice — `@WebMvcTest`
Controller + serialization + validation + security rules, service mocked.
- Request validation → 400 with the standard error body.
- Auth: no token → 401; wrong role on admin route → 403.
- Correct status codes (201 on create, 402/409 on domain failures).
- Use `MockMvc` (+ `@WithMockUser`/jwt post-processor).

### 2.3 Persistence slice — `@DataJpaTest`
Repositories against a **real MySQL via Testcontainers** (not H2 — H2 hides
MySQL-specific behaviour). Test custom queries, unique constraints (duplicate
email, one review per user/product), pagination, and `@Version` optimistic lock.

### 2.4 Integration tests — `@SpringBootTest(webEnvironment=RANDOM_PORT)`
Whole service wired up, real DB (Testcontainers), **external services stubbed
with WireMock**.
- Full request→controller→service→repo→DB path via `TestRestTemplate`/`WebTestClient`.
- Feign consumers (shop): point the client at WireMock; assert correct request
  bodies, header propagation (`X-User-Id`, `idempotencyKey`), and mapping of
  200/402/409/5xx responses.
- Security end-to-end within the service (real JWT filter, real token).

### 2.5 Resilience tests
- With WireMock returning errors/delays, assert the **fallback** runs and the
  **circuit breaker opens** after the threshold (inspect
  `/actuator/circuitbreakerevents` or the `CircuitBreakerRegistry`).
- Assert money-safety invariants under failure: a failed debit **always** results
  in released stock and a FAILED order — never a confirmed unpaid order.
- Assert idempotency: a retried debit (same key) does not double-charge.

### 2.6 End-to-end (whole system)
A separate test/profile that boots the real mesh (or uses `docker-compose` with
all six services + MySQL) and drives the **gateway**:
`register → login → browse products → add to cart → checkout → verify wallet
debited, stock decremented, order CONFIRMED → post review → cancel → verify
refund + stock released`. Keep few but meaningful; they are the proof the system
actually works together.

---

## 3. Tooling
| Purpose | Tool |
|---------|------|
| Test framework | JUnit 5 (Jupiter) |
| Mocking | Mockito |
| Assertions | AssertJ (fluent) |
| Web tests | MockMvc / WebTestClient |
| Real DB in tests | **Testcontainers (MySQL)** |
| Stub external HTTP (Feign) | **WireMock** |
| Coverage | **JaCoCo** (Maven plugin) |
| JSON assertions | JsonPath / JSONAssert |

> Add Testcontainers, WireMock, and the JaCoCo plugin to the parent `pom.xml` in
> Phase 0 so every module inherits them. (Spring Boot's dependency management
> already knows their versions.)

## 4. Coverage & gates
- **Target: ≥ 80% line coverage** on `service/`, `domain/`, saga, and mapper
  packages (the logic). Controllers/config need not hit the same bar but must
  have slice tests. Coverage is a guardrail, not the goal — **branch coverage of
  business rules matters more than a headline %.**
- Configure JaCoCo to **fail the build** below the threshold.
- `./mvnw clean verify` runs unit + integration tests + coverage for all modules.
  A green `verify` is the definition of "the build passes".

## 5. Conventions
- Naming: `methodUnderTest_condition_expectedResult`
  (`debit_whenBalanceTooLow_throwsInsufficientFunds`).
- Arrange–Act–Assert structure; one behaviour per test.
- Test data builders / object mothers instead of copy-pasted setup.
- Deterministic: no reliance on wall-clock, ordering, or shared mutable state.
  Fix time via `Clock` injection where relevant.
- Unit tests must not touch network/DB/disk. Integration tests own that.

## 6. Definition of Done (applies to every phase)
A task/phase is complete only when:
1. New logic has unit tests covering success **and** failure branches.
2. Controllers have `@WebMvcTest` slices; repositories have `@DataJpaTest` slices.
3. Cross-service behaviour has integration tests (WireMock) and, for checkout,
   resilience + idempotency tests.
4. `./mvnw clean verify` is **green** and coverage meets the threshold.
5. The happy path is demonstrable via the E2E flow / Postman collection.
