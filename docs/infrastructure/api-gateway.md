# API Gateway

The single public entry point (`:8080`). Clients never call services directly.
The gateway is also where we enforce the security boundary — validating the JWT
once at the edge and hiding internal service-to-service endpoints from the outside
world.

## Module
`api-gateway` — Spring Cloud Gateway, port **8080**, name `api-gateway`, Eureka
client. Uses discovery-based routing (`lb://`).

## Responsibilities
1. **Routing** — map public paths to services by Eureka name.
2. **Load balancing** — `lb://` uses Spring Cloud LoadBalancer across instances.
3. **Auth gate** — validate the JWT once at the edge and forward the user id
   downstream as `X-User-Id` (see [../security/authentication-authorization.md](../security/authentication-authorization.md)).
4. **Hide internals** — never expose service-to-service endpoints.

## Routes

```yaml
spring:
  cloud:
    gateway:
      discovery.locator.enabled: false   # define explicit routes, don't auto-expose everything
      routes:
        - id: wallet-auth
          uri: lb://wallet-service
          predicates: [ "Path=/api/v1/auth/**" ]        # public
        - id: wallet
          uri: lb://wallet-service
          predicates: [ "Path=/api/v1/wallets/me/**,/api/v1/users/**" ]
        - id: inventory-public
          uri: lb://inventory-service
          predicates: [ "Path=/api/v1/products/**,/api/v1/categories/**" ]
        - id: shop
          uri: lb://shop-service
          predicates: [ "Path=/api/v1/cart/**,/api/v1/wishlist/**,/api/v1/orders/**,/api/v1/reviews/**" ]
```

### Must NOT be routed (internal only)
`/wallets/{userId}/debit|credit|balance`, `/inventory/check|reserve|release`,
`/products/batch`, and admin write routes unless you deliberately expose an admin
surface. Because routes are **explicit** (locator disabled), anything not listed
is simply unreachable from outside — which is what we want.

## Auth filter
A global filter (or per-route filter):
- Skips `/api/v1/auth/**` and public GET catalog routes.
- For everything else: read `Authorization: Bearer`, validate signature/expiry
  with the shared `JWT_SECRET`, reject with 401 if bad, else set request header
  `X-User-Id` from the `sub` claim before forwarding.

> Reactive note: Spring Cloud Gateway is WebFlux-based. Keep the filter reactive
> (no blocking JWT/DB calls). JWT validation is pure CPU (no I/O), so it's fine.

## Cross-cutting (nice-to-have)
- CORS config (for a future frontend).
- Request logging / correlation id.
- A Resilience4j route filter as a second layer (primary breakers live in
  shop-service around Feign).

## Verify
```bash
# public
curl http://localhost:8080/api/v1/products
# protected without token -> 401
curl -i http://localhost:8080/api/v1/cart
# with token -> 200
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/cart
```
