# Distributed E-Commerce Microservices Platform

A production-grade, distributed microservices platform for e-commerce, built with **Spring Boot 3**, **Spring Cloud (2025)**, a **database-per-service** architecture, **JWT stateless security**, a **synchronous checkout saga with compensation on failure**, **OpenFeign** inter-service calls, **Resilience4j** fault tolerance, and an **API Gateway** edge auth filter.

---

## Architecture & Microservice Topology

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

### Infrastructure Services
- **`config-server`** (Port `8888`): Centralized native configuration server delivering environment-specific YAML configs with zero committed secrets.
- **`eureka-server`** (Port `8761`): Netflix Eureka service discovery registry.
- **`api-gateway`** (Port `8080`): Edge gateway providing path routing, JWT validation, and `X-User-Id`/`X-User-Roles` header injection; internal service-to-service endpoints are not routed publicly.

### Business Microservices
- **`inventory-service`** (Port `8081`): Product catalog, category management, stock reservations & releases (`inventory_db`).
- **`wallet-service`** (Port `8082`): Auth provider (JWT issuer), user management, wallet balance, deposits/withdrawals, and idempotent debits & credits (`wallet_db`).
- **`shop-service`** (Port `8083`): Cart with live price enrichment, wishlist, reviews, order management, and the checkout saga (`shop_db`).

---

## Technical Highlights & Key Engineering Patterns

1. **Distributed Saga Pattern (Checkout Saga)**:
   - Synchronous multi-step transaction orchestrating stock reservation (`inventory-service`) and wallet debit (`wallet-service`).
   - Automated compensation logic (`inventory/release`) triggered on payment failure to ensure strict eventual data consistency.

2. **Fault Tolerance & Resilience**:
   - Resilience4j `@CircuitBreaker`, `@Retry`, and `@TimeLimiter` wrappers around inter-service OpenFeign calls.
   - Business error separation (409 Out of Stock, 402 Insufficient Funds mapped to business exceptions) preventing transient circuit breaker trips.

3. **Stateless JWT Security Architecture**:
   - `wallet-service` acts as the single Identity Provider (IdP) issuing signed JWTs.
   - `api-gateway` validates tokens at the perimeter, injecting authenticated user metadata (`X-User-Id`, `X-User-Roles`) into downstream headers.
   - Internal inter-service endpoints (`/inventory/reserve`, `/wallets/*/debit`) are shielded from public route exposure.

4. **Testing Strategy & High Coverage**:
   - Comprehensive test suite including unit tests, slice tests with Testcontainers (real MySQL), inter-service WireMock stubs, and full reactor verification (`./mvnw clean verify`).

---

## Quick Start Guide

### Prerequisites
- **JDK 17+**
- **MySQL 8.x** running locally (or via Docker) with `inventory_db`, `wallet_db`, and `shop_db` created.

### Build & Run Tests
```bash
# Runs the full unit, slice (Testcontainers), and integration (WireMock) suite
./mvnw clean verify
```

### Startup Options

#### Option A: Docker Compose (All Services + MySQL)
```bash
# Package all jars first
./mvnw clean package -DskipTests

# Build images and start all 6 microservices + MySQL
docker compose up --build -d
```

#### Option B: Sequential Maven / IDE Startup
Start the microservices in the following order:
1. `config-server` (`:8888`): `./mvnw -pl config-server spring-boot:run`
2. `eureka-server` (`:8761`): `./mvnw -pl eureka-server spring-boot:run`
3. `inventory-service` (`:8081`): `./mvnw -pl inventory-service spring-boot:run`
4. `wallet-service` (`:8082`): `./mvnw -pl wallet-service spring-boot:run`
5. `shop-service` (`:8083`): `./mvnw -pl shop-service spring-boot:run`
6. `api-gateway` (`:8080`): `./mvnw -pl api-gateway spring-boot:run`

---

## Environment Variables & Configuration

Sensitive and environment-specific settings are configured via placeholders:

| Variable | Description | Example / Default |
|---|---|---|
| `JWT_SECRET` | Shared secret for signing & verifying JWTs (min 256 bits) | `YourSuperSecretKeyMustBe32BytesOrLonger!` |
| `DB_URL` | JDBC Connection URL | `jdbc:mysql://localhost:3306/shop_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `root` |
| `CONFIG_SERVER_URL` | Config server base URL | `http://localhost:8888` |
| `EUREKA_URL` | Eureka registration URL | `http://localhost:8761/eureka` |

---

## Postman Collection & Documentation

- **Postman Collection**: [docs/postman/ecommerce-platform.postman_collection.json](docs/postman/ecommerce-platform.postman_collection.json)
- **Postman Environment**: [docs/postman/ecommerce-platform.postman_environment.json](docs/postman/ecommerce-platform.postman_environment.json)
- **Operations & Running Guide**: [docs/running-and-extending.md](docs/running-and-extending.md)
- **Design Decisions & Architecture**: [docs/architecture/00-design-decisions.md](docs/architecture/00-design-decisions.md)
- **Request Flows & Checkout Saga**: [docs/architecture/03-request-flows.md](docs/architecture/03-request-flows.md)
- **Authentication & Security**: [docs/security/authentication-authorization.md](docs/security/authentication-authorization.md)

