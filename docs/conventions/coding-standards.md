# Coding Standards

Consistency across the three services matters more than personal preference.

## Package structure (per service)
```
com.ejada.ecommerce.<service>
├── config/         # Spring @Configuration, security, feign, resilience
├── controller/     # @RestController — thin, no business logic
├── service/        # business logic (interfaces + impl)
├── repository/     # Spring Data JPA interfaces
├── domain/  (or entity/)  # @Entity classes
├── dto/            # request/response records
├── client/         # Feign clients (+ fallbacks)
├── exception/      # custom exceptions + @RestControllerAdvice
└── mapper/         # entity <-> dto mapping
```

## Layering rules
- **Controller** → validates input, calls a service, returns a DTO. No JPA here.
- **Service** → holds logic and `@Transactional` boundaries.
- **Repository** → Spring Data interfaces only.
- **Never return JPA entities from controllers.** Map to DTOs (avoids lazy-load
  serialization bugs and leaking internal fields).

## DTOs
- Use Java `record`s for request/response DTOs.
- Validate requests with Jakarta annotations (`@NotNull`, `@Positive`, `@Email`,
  `@Size`) + `@Valid` on the controller parameter.

## Dependency injection
- **Constructor injection** only (with Lombok `@RequiredArgsConstructor`). No
  field `@Autowired`.

## Error handling
- One `@RestControllerAdvice` per service producing a consistent body:
  ```json
  { "timestamp": "...", "status": 409, "error": "OUT_OF_STOCK",
    "message": "…", "path": "/api/v1/orders" }
  ```
- Throw domain exceptions (`InsufficientFundsException`,
  `ProductNotFoundException`) and map them to HTTP codes in the advice. No
  `try/catch` that swallows and returns 200.

## Money & time
- Money: `BigDecimal` always; never `double`/`float`. Store currency alongside.
- Time: `Instant`/`OffsetDateTime` (UTC). Let JPA auditing set `createdAt/updatedAt`.

## Persistence
- Explicit `@Column` names; explicit table names. `FetchType.LAZY` for
  associations. Avoid `CascadeType.ALL` unless the child truly can't exist alone.
- Use `@Version` where concurrent updates matter (wallet balance, stock).

## Naming
- REST: plural nouns, kebab-free lowercase (`/products`, `/wallets/me`). Versioned
  under `/api/v1`.
- Java: `PascalCase` types, `camelCase` members, `SCREAMING_SNAKE` constants.

## Logging
- SLF4J (`@Slf4j`). Log at boundaries and failures. **Never log secrets, tokens,
  passwords, full card/PII.** Include a correlation id if available.

## Config
- No hard-coded ports/URLs/secrets in code — read from config. Placeholders only
  in committed YAML (`${JWT_SECRET}`).

## Formatting
- Follow standard Java conventions / IDE default (Spring style). Keep methods
  small; one responsibility each. Match the style of surrounding code.
