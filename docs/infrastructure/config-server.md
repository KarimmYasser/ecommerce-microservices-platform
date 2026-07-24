# Config Server

Centralises configuration so the five clients (eureka, gateway, and the three
business services) don't each carry their own copy — one place to change a port,
a datasource, or a resilience threshold. We stand it up **early** in the topology
(a boot-time dependency), not as a late add-on — see
[DEV-06](../architecture/00-design-decisions.md#dev-06--config-server-comes-up-first-not-last).

## Module
`config-server` — `@EnableConfigServer`, port **8888**, name `config-server`.

## Backend: native (filesystem) vs git
For a course project use the **native** profile pointing at a local folder, which
avoids needing a separate config git repo:

```yaml
# config-server/src/main/resources/application.yml
server.port: 8888
spring:
  application.name: config-server
  profiles.active: native
  cloud.config.server.native.search-locations: classpath:/config
```

Config files live under `config-server/src/main/resources/config/`:
```
config/
├── application.yml            # shared defaults for everyone
├── inventory-service.yml
├── wallet-service.yml
├── shop-service.yml
├── api-gateway.yml
└── eureka-server.yml
```
A client named `inventory-service` automatically receives `application.yml`
(shared) merged with `inventory-service.yml` (specific).

## How clients consume it
Each client adds `spring-cloud-starter-config` and imports config at boot:

```yaml
# in every client's application.yml
spring:
  application.name: inventory-service
  config.import: "optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
```

Precedence (highest wins): environment variables / local override →
config-server values → packaged defaults.

## Secrets — the rule
- **No secret values in the committed config files.** Use placeholders that
  resolve from the environment, e.g.:
  ```yaml
  spring.datasource.password: ${DB_PASSWORD}
  jwt.secret: ${JWT_SECRET}
  ```
- Real values come from env vars (or a git-ignored `application-local.yml` next
  to each service). See [../conventions/agent-rules.md](../conventions/agent-rules.md).
- (Production would use Spring Cloud Config encryption or Vault — out of scope,
  but mention it in the report.)

## Startup
Config-server must be **first** up. Clients use `optional:` so they can still
boot for isolated testing if it's absent, but for the full run start it first.

## Verify
```bash
curl http://localhost:8888/inventory-service/default   # returns merged config JSON
```
