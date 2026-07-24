# Ejada E-Commerce Platform — Microservices Backend

A Spring Cloud microservices platform for an e-commerce store, built with a
database-per-service architecture, JWT stateless security, a synchronous
checkout saga with compensation on failure, OpenFeign inter-service calls, and
an API Gateway edge auth filter.

> **Status:** Phases 0–3 done and fully tested; 175 tests green across the
> full reactor (`./mvnw clean verify`). Phases 4–5 (service mesh, end-to-end
> validation) are mostly done — see
> [docs/implementation-plan/README.md](docs/implementation-plan/README.md) for
> the current status board. One open item: Resilience4j circuit
> breaker/retry/time-limiter instances are configured and visible via
> Actuator, but nothing is wired to them yet with `@CircuitBreaker`/`@Retry`
> — the checkout saga's resilience today comes from manually inspecting
> `FeignException` status codes and running the matching compensation, not
> from an automated breaker. See
> [docs/implementation-plan/phase-4-service-mesh.md](docs/implementation-plan/phase-4-service-mesh.md).

---

## Architecture & microservice topology

The system comprises 6 deployable microservices orchestrated via Maven parent POM:

```
                      +-------------------+
                      |   Client / UI     |
                      +---------+---------+
                                | :8080
                                v
                      +-------------------+
                      |    API Gateway    |
                      +---------+---------+
                                |
       +------------------------+------------------------+
       | lb://                  | lb://                  | lb://
       v                        v                        v
+--------------+        +--------------+        +--------------+
|   Inventory  |        |    Wallet    |        |     Shop     |
|   Service    |        |   Service    |        |   Service    |
|   (:8081)    |        |   (:8082)    |        |   (:8083)    |
+------+-------+        +------+-------+        +------+-------+
       |                       |                       |
       v                       v                       v
[inventory_db]            [wallet_db]              [shop_db]
```

### Infrastructure services
- **`config-server`** (Port `8888`): centralised native configuration server delivering environment-specific YAML configs with zero committed secrets.
- **`eureka-server`** (Port `8761`): Netflix Eureka service discovery registry.
- **`api-gateway`** (Port `8080`): edge gateway providing path routing, JWT validation, and `X-User-Id`/`X-User-Roles` header injection; internal service-to-service endpoints are not routed publicly.

### Business microservices
- **`inventory-service`** (Port `8081`): product catalog, category management, stock reservations & releases (`inventory_db`).
- **`wallet-service`** (Port `8082`): auth provider (JWT issuer), user management, wallet balance, deposits/withdrawals, and idempotent debits & credits (`wallet_db`).
- **`shop-service`** (Port `8083`): cart with live price enrichment, wishlist, reviews, order management, and the checkout saga (`shop_db`).

---

## Quick start guide

### Prerequisites
- **JDK 17** or higher
- **MySQL 8.x** running locally (or via Docker):
  - `inventory_db` (Port 3306)
  - `wallet_db` (Port 3306)
  - `shop_db` (Port 3306)

### Build & run tests
```bash
# Runs the full unit, slice (Testcontainers), and integration (WireMock) suite
./mvnw clean verify
```

### Startup order (local development)
Start the microservices in the following sequential order:
1. `config-server` (`:8888`)
2. `eureka-server` (`:8761`)
3. `inventory-service` (`:8081`)
4. `wallet-service` (`:8082`)
5. `shop-service` (`:8083`)
6. `api-gateway` (`:8080`)

---

## Security & environment variables

All sensitive values are passed via environment variables (never committed):

| Variable | Description | Example / Default |
|---|---|---|
| `JWT_SECRET` | Shared secret for signing & verifying JWTs (min 256 bits) | `YourSuperSecretKeyMustBe32BytesOrLonger!` |
| `DB_URL` | JDBC Connection URL | `jdbc:mysql://localhost:3306/shop_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `root` |
| `CONFIG_SERVER_URL` | Config server base URL | `http://localhost:8888` |
| `EUREKA_URL` | Eureka registration URL | `http://localhost:8761/eureka` |

---

## Postman collection

A Postman collection and environment are under `docs/postman/`:
- **Collection**: [docs/postman/ecommerce-platform.postman_collection.json](docs/postman/ecommerce-platform.postman_collection.json)
- **Environment**: [docs/postman/ecommerce-platform.postman_environment.json](docs/postman/ecommerce-platform.postman_environment.json)

The `Auth -> Login User` request's test script extracts the returned JWT into
the `{{jwt}}` collection variable, which subsequent protected requests reuse.
This is a manual demo aid, not a replacement for the automated test suite.

---

## Endpoints & Actuator dashboards

- **Eureka service registry**: `http://localhost:8761`
- **Config server endpoints**: `http://localhost:8888/shop-service/default`
- **Swagger UI**:
  - `inventory-service`: `http://localhost:8081/swagger-ui.html`
  - `wallet-service`: `http://localhost:8082/swagger-ui.html`
  - `shop-service`: `http://localhost:8083/swagger-ui.html`
- **Actuator (shop-service)**: circuit breaker instances are registered and
  visible at `/actuator/circuitbreakers` and `/actuator/circuitbreakerevents`,
  but nothing calls through them yet — see the status note above.

---

## Project documentation

- [Project Guidelines & Architecture Rules](CLAUDE.md)
- [Design Decisions (`00-design-decisions.md`)](docs/architecture/00-design-decisions.md)
- [Request Flows & Checkout Saga (`03-request-flows.md`)](docs/architecture/03-request-flows.md)
- [Authentication & Authorization (`authentication-authorization.md`)](docs/security/authentication-authorization.md)
- [Implementation Roadmap Status Board](docs/implementation-plan/README.md)
