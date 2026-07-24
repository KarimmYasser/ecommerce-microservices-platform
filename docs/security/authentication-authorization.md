# Authentication & Authorization

## Model: stateless JWT, issued by wallet-service

- **wallet-service** is the identity provider. On successful login it signs a JWT
  with a **shared symmetric secret** (`JWT_SECRET`, HS256).
- The token's `sub` claim = `userId`; a `roles` claim = `["USER"]` / `["ADMIN"]`.
- No server-side session. Every request carries `Authorization: Bearer <token>`.

```json
// JWT payload
{ "sub": "5", "roles": ["USER"], "iat": 1750000000, "exp": 1750003600 }
```

## Who validates the token?

Two layers, both using the same `JWT_SECRET` from config:

1. **Gateway (edge):** validates signature + expiry for all non-public routes,
   rejects invalid tokens with 401, and forwards the trusted `X-User-Id`
   (and `X-User-Roles`) header downstream.
2. **Each business service:** a lightweight Spring Security filter also validates
   the token (defence in depth) — or, if you trust the mesh, reads `X-User-Id`.
   For the course, validating the JWT in each service with the shared secret is
   the clearest and safest; document whichever you choose.

> Decide once and be consistent: **(A)** every service validates the JWT itself
> (recommended, simplest to reason about), or **(B)** only the gateway validates
> and services trust `X-User-Id`. Pick (A) unless you lock down the network.

## Public vs protected

| Public (no token) | Protected (USER) | Admin (ADMIN) |
|-------------------|------------------|---------------|
| `POST /auth/register`, `POST /auth/login` | cart, wishlist, orders, reviews, `/wallets/me/**`, `/users/me` | product/category writes, stock adjust |
| `GET /products/**`, `GET /categories/**` | | |

Enforce with Spring Security `authorizeHttpRequests` per service +
`@PreAuthorize("hasRole('ADMIN')")` on admin methods.

## Password storage
- BCrypt via `PasswordEncoder`. Never store or log plaintext.

## The secret
- `JWT_SECRET` is a **runtime secret** — env var / git-ignored local file only,
  never committed. Same value across wallet-service, gateway, and any service
  that validates tokens (delivered via config-server placeholder `${JWT_SECRET}`).
- Use a strong random ≥ 32 bytes for HS256.

## Identity in requests
Services take `userId` from the **validated token / `X-User-Id`**, never from a
body or query param. This prevents a user acting as someone else.

## Library
`io.jsonwebtoken:jjwt` (api/impl/jackson) for signing & parsing. Add to
wallet-service (sign + validate), gateway and other services (validate only).

## Verify
- Register + login → receive token.
- Call a protected route without token → 401; with token → 200.
- Call an admin route as a USER → 403.
