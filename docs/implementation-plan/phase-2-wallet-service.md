# Phase 2 — wallet-service

Identity + money. Independent of inventory; can be built in parallel with Phase 1.

**Read first:** [domain/wallet.md](../domain/wallet.md) ·
[api/wallet-api.md](../api/wallet-api.md) ·
[security/authentication-authorization.md](../security/authentication-authorization.md).

**Status: done.** Verified 2026-07-24 — 54 tests green, full 6-module reactor
build stays green at 130 tests total. See the Definition of Done below.

## Tasks
- [x] Entities: `User`, `Wallet`, `WalletTransaction` (with `@Version` on Wallet,
      unique `idempotencyKey` on transactions).
- [x] Auth: `POST /auth/register` (create user + empty wallet in one transaction,
      BCrypt password), `POST /auth/login` (verify + sign JWT with `JWT_SECRET`).
- [x] JWT: signing (jjwt), a validation filter, `PasswordEncoder` bean.
- [x] Profile: `GET/PUT /users/me` (identity from token).
- [x] Wallet: `GET /wallets/me`, `deposit`, `withdraw`, `GET .../transactions`.
- [x] Internal (Feign-facing): `POST /wallets/{userId}/debit` (idempotent),
      `credit`, `GET .../balance`. Debit/credit mutate balance **and** append a
      ledger row in the same `@Transactional` unit; never go below zero.
- [x] `@RestControllerAdvice`: insufficient funds → 402, duplicate email → 409.

## A deliberate deviation from the domain doc: pessimistic lock, not `@Version` retry
The domain doc lists `@Version` as the primary concurrency mechanism ("or
`SELECT ... FOR UPDATE`" as the alternative). `@Version` is still on the
`Wallet` entity, but the service layer's actual concurrency guard is a
**pessimistic write lock** (`WalletRepository.findByUserIdForUpdate`), the same
pattern already proven in Phase 1's `StockItemRepository`. Reasoning: making
`@Version` the primary mechanism requires retrying on
`ObjectOptimisticLockingFailureException` with a **fresh transaction** per
attempt, which runs straight into Spring's self-invocation limitation (a
`@Transactional` method calling another `@Transactional` method on `this`
bypasses the proxy, so naive in-class retry loops silently don't get a new
transaction). Pessimistic locking sidesteps that entirely — concurrent
debits/credits on the same wallet simply serialize at the database, each
sees a genuinely fresh balance, and no retry logic is needed at all. This was
a considered choice, not an oversight; noted here so nobody "fixes" it back
to a broken retry loop later.

## Tests — 54 total, all green
- [x] **Unit (18):** `WalletServiceImplTest` (9 — deposit/withdraw math,
      insufficient-funds guard leaves balance untouched, debit/credit
      idempotent replay returns the prior result without re-mutating),
      `AuthServiceImplTest` (6 — register/duplicate-email, login success/
      wrong-password/unknown-email/disabled-user), `UserServiceImplTest` (3).
- [x] **Mapper unit tests (2).**
- [x] **@DataJpaTest (7, Testcontainers MySQL):** `UserRepositoryTest` (3 —
      unique email constraint), `WalletRepositoryTest` (4 — unique
      idempotencyKey constraint, `@Version` genuinely increments on update,
      ledger ordering).
- [x] **@WebMvcTest (19):** all four controllers — validation, 401 without a
      token, 402 on overdraw, 409 on duplicate email, 401 on bad credentials.
- [x] **@SpringBootTest integration (7, live embedded server + Testcontainers):**
      unlike inventory-service (which has to mint a JWT itself since it isn't
      an issuer), this suite drives the **real** `/auth/register` +
      `/auth/login` endpoints to obtain a genuine token. Covers the full
      register→login→profile→deposit→withdraw→transactions lifecycle,
      internal debit/credit idempotency, 402-with-no-mutation on insufficient
      funds, duplicate-email 409, wrong-password 401, protected-route-without-
      token 401, and a **5-thread concurrent-debit test** (100 available
      balance, five concurrent 30-unit debits) asserting exactly 3 succeed and
      the final balance is exactly 10 — never negative.
- [x] `./mvnw -pl wallet-service verify` green. Full 6-module
      `./mvnw clean verify` also green (130 tests total across the reactor).

## Notable follow-up (non-blocking)
`RestTemplateBuilder.rootUri(String)` — used to point `TestRestTemplate` at the
random test port — is deprecated as of Boot 4.1 and marked for removal. Still
works today; whichever phase next touches the integration test harness should
switch to the replacement API rather than leaving it for a version bump to
break silently.

## Done when
A user can register, log in, receive a JWT, top up, and the internal
debit/credit endpoints move money safely and idempotently — proven by tests
including the concurrency and idempotency cases. ✅
