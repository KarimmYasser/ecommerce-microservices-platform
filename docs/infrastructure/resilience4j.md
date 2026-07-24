# Resilience4j — Circuit Breakers

Protects `shop-service` from failures in `inventory-service`/`wallet-service`
during checkout: without breakers, a slow or dead dependency would hang the saga
and cascade failure upstream. Uses `spring-cloud-starter-circuitbreaker-resilience4j`
(already in `pom.xml`). This directly supports the consistency guarantees of the
checkout saga ([DEV-09](../architecture/00-design-decisions.md#dev-09--checkout-is-an-orchestrated-saga-with-compensation--idempotency)).

## Where breakers go
Around **every Feign call from shop-service**. Each external dependency gets its
own breaker instance so a failing wallet doesn't trip the inventory breaker.

| Breaker | Wraps | Fallback |
|---------|-------|----------|
| `inventoryCheck` | `GET /products/batch`, `POST /inventory/check` | serve cached/snapshot data, mark "unconfirmed" |
| `inventoryReserve` | `POST /inventory/reserve` | **fail checkout** (no fake success) |
| `walletDebit` | `POST /wallets/{id}/debit` | **fail checkout** + release reservation |
| `walletCredit`/`inventoryRelease` | refund/compensation | queue for **retry**, never drop |

> Golden rule: fallbacks may **degrade reads**, but must **never fabricate a
> successful payment or stock reservation**. Compensations are retried, not
> faked. See [../api/inter-service-feign.md](../api/inter-service-feign.md).

## Configuration (in each service's config)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      walletDebit:
        sliding-window-size: 10
        failure-rate-threshold: 50          # open at 50% failures
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        register-health-indicator: true
  timelimiter:
    instances:
      walletDebit: { timeout-duration: 2s }
  retry:
    instances:
      inventoryRelease: { max-attempts: 3, wait-duration: 500ms }
```

## Applying it (two options)
- **Annotation:** `@CircuitBreaker(name="walletDebit", fallbackMethod="debitFallback")`
  on the service method that calls the Feign client (+ `@Retry`/`@TimeLimiter` as needed).
- **Programmatic:** inject `CircuitBreakerFactory` and wrap the call.

Prefer the annotation style for readability; keep the fallback method signature
matching (same args + a trailing `Throwable`).

## Monitoring — via Actuator/Micrometer (see [DEV-10](../architecture/00-design-decisions.md#dev-10--resilience-monitoring-via-actuatormicrometer-not-the-resilience4j-dashboard))
- Expose actuator: `management.endpoints.web.exposure.include: health,circuitbreakers,circuitbreakerevents`
  and `management.health.circuitbreakers.enabled: true`.
- Inspect at `/actuator/circuitbreakers` and `/actuator/circuitbreakerevents`.
- (Optional) point Prometheus/Grafana or the Resilience4j dashboard at these.

## Testing the breaker (simulated outage)
1. Start everything, confirm a normal checkout works.
2. **Stop wallet-service.**
3. Attempt checkout → shop-service should: hit the `walletDebit` fallback,
   release the inventory reservation, return `PAYMENT_UNAVAILABLE` (not hang).
4. After several failures the breaker **opens** — subsequent calls fail fast
   (visible in `/actuator/circuitbreakerevents`).
5. Restart wallet-service → breaker transitions half-open → closed; checkout
   works again.
Document this scenario with screenshots in the final report.
