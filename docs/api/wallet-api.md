# API — wallet-service

Base path: `/api/v1`. This service also issues JWTs (auth endpoints are public;
everything else needs a valid token).

---

## Auth (public)

| Method | Path | Purpose | Figma |
|--------|------|---------|-------|
| POST | `/auth/register` | Create user + empty wallet | Sign-up |
| POST | `/auth/login` | Verify credentials, return JWT | Login / My Account |

**`POST /auth/register` body**
```json
{ "email": "a@b.com", "password": "secret123", "fullName": "Ahmed", "phone": "+20..." }
```
→ **201** `{ "userId": 5 }`

**`POST /auth/login` body** `{ "email", "password" }`
→ **200** `{ "accessToken": "<jwt>", "tokenType": "Bearer", "expiresIn": 3600 }`

---

## Profile (authenticated — identity from JWT)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/users/me` | Current user's profile |
| PUT | `/users/me` | Update name/phone |

---

## Wallet (authenticated)

| Method | Path | Purpose | Figma |
|--------|------|---------|-------|
| GET | `/wallets/me` | Balance + currency | (account/checkout) |
| POST | `/wallets/me/deposit` | Add funds | top-up |
| POST | `/wallets/me/withdraw` | Remove funds | — |
| GET | `/wallets/me/transactions` | Paged ledger | transaction history |

**`POST /wallets/me/deposit` / `withdraw` body**
```json
{ "amount": 500.00 }
```
→ **200** `{ "balance": 1500.00, "transactionId": 88 }`
Withdraw beyond balance → **402** `{ "error": "INSUFFICIENT_FUNDS" }`.

**`GET /wallets/me/transactions` 200** — paged list of
`{ id, type, amount, balanceAfter, referenceId, status, createdAt }`.

---

## Internal (called by shop via Feign — full spec in [inter-service-feign.md](inter-service-feign.md))

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/wallets/{userId}/debit` | Charge for an order (idempotent) |
| POST | `/wallets/{userId}/credit` | Refund on cancellation |
| GET | `/wallets/{userId}/balance` | Read balance for a user |

`debit` body includes an `idempotencyKey` (the order id) so retries never
double-charge — see the ledger rules in [../domain/wallet.md](../domain/wallet.md).

---

## Errors
| Status | When |
|--------|------|
| 400 | validation (negative amount, bad email) |
| 401 | missing/invalid token |
| 402 | insufficient funds (withdraw/debit) |
| 409 | duplicate email on register |
