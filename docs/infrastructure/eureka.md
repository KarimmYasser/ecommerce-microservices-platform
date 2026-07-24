# Eureka — Service Discovery

Lets services find each other by **name** instead of hard-coded host:port, and
lets the gateway/Feign load-balance across instances. This is what makes the
database-per-service, call-by-name architecture
([DEV-01](../architecture/00-design-decisions.md#dev-01--database-per-service-never-a-shared-schema))
practical at runtime.

## Server module
`eureka-server` — `@EnableEurekaServer`, port **8761**, name `eureka-server`.

```yaml
server.port: 8761
spring.application.name: eureka-server
eureka:
  client:
    register-with-eureka: false   # the registry doesn't register with itself
    fetch-registry: false
  server:
    enable-self-preservation: false   # off in dev so dead instances drop quickly
```

Dashboard: <http://localhost:8761> — you should see every service listed once it's up.

## Clients
Every other module (gateway + 3 services + config-server optionally) adds
`spring-cloud-starter-netflix-eureka-client` and:

```yaml
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka}
  instance:
    prefer-ip-address: true   # simpler on Windows/localhost
```

Nothing else is required — `spring.application.name` becomes the registry id
used by the gateway (`lb://inventory-service`) and by Feign clients.

## How it's used downstream
- **Gateway** routes `lb://shop-service` → a live shop instance.
- **Feign** clients in shop-service resolve `inventory-service`/`wallet-service`
  by name and client-side load-balance (Spring Cloud LoadBalancer).

## Verify
1. Start eureka, then a service.
2. Open the dashboard; confirm the service appears as `UP`.
3. From shop-service, a Feign call by name should succeed without any URL.
