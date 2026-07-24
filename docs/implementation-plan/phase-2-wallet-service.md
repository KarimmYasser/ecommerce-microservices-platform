# Phase 2 — wallet-service

Identity + money. Independent of inventory; can be built in parallel with Phase 1.

**Read first:** [domain/wallet.md](../domain/wallet.md) ·
[api/wallet-api.md](../api/wallet-api.md) ·
[security/authentication-authorization.md](../security/authentication-authorization.md).

## Tasks
- [ ] Entities: `User`, `Wallet`, `WalletTransaction` (with `@Version` on Wallet,
      unique `idempotencyKey` on transactions).
- [ ] Auth: `POST /auth/register` (create user + empty wallet in one transaction,
      BCrypt password), `POST /auth/login` (verify + sign JWT with `JWT_SECRET`).
- [ ] JWT: signing (jjwt), a validation filter, `PasswordEncoder` bean.
- [ ] Profile: `GET/PUT /users/me` (identity from token).
- [ ] Wallet: `GET /wallets/me`, `deposit`, `withdraw`, `GET .../transactions`.
- [ ] Internal (Feign-facing): `POST /wallets/{userId}/debit` (idempotent),
      `credit`, `GET .../balance`. Debit/credit mutate balance **and** append a
      ledger row in the same `@Transactional` unit; never go below zero.
- [ ] `@RestControllerAdvice`: insufficient funds → 402, duplicate email → 409.

## Tests (Definition of Done includes these)
- [ ] **Unit:** debit success / insufficient funds / **idempotent replay** (same
      key → prior result, no double-charge); withdraw guards; balance-after math;
      optimistic-lock conflict path.
- [ ] **@DataJpaTest (Testcontainers):** unique email, unique idempotencyKey,
      ledger append, `@Version` increment.
- [ ] **@WebMvcTest:** register/login validation, 401 on protected routes without
      token, 402 on overdraw.
- [ ] **Security integration:** real token issued by login validates on a
      protected endpoint; USER hitting an ADMIN route → 403.
- [ ] **Concurrency test:** two parallel debits on the same wallet don't
      double-spend (one succeeds, one retries/fails cleanly).
- [ ] `./mvnw -pl wallet-service verify` green; coverage ≥ threshold.

## Done when
A user can register, log in, receive a JWT, top up, and the internal debit/credit
endpoints move money safely and idempotently — proven by tests including the
concurrency and idempotency cases.
