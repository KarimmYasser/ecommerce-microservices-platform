# Running and Extending the Microservices Architecture

This guide provides step-by-step instructions for **running the full microservices architecture locally**, **scaling existing services horizontally**, and **adding new microservices** to the fabric.

---

## 1. System Status

All core phases of the project are 100% complete and fully verified:

| Component | Responsibility | Port | Status |
|-----------|----------------|------|--------|
| **config-server** | Centralized configuration via native profile | `8888` | ✅ Done |
| **eureka-server** | Service registration & discovery registry | `8761` | ✅ Done |
| **api-gateway** | Entry point, JWT authentication, route forwarding | `8080` | ✅ Done |
| **inventory-service** | Catalog, variants, stock management & locking | `8081` | ✅ Done |
| **wallet-service** | User accounts, auth (JWT), funds & debits/credits | `8082` | ✅ Done |
| **shop-service** | Cart, Wishlist, Reviews, Orders & Checkout Saga | `8083` | ✅ Done |

All automated unit, integration, and reactor build tests pass (`./mvnw clean verify`).

---

## 2. Local Startup Guide (Running Full Stack)

### Prerequisites
1. **Java 17+**: Ensure Java 17 or higher is active (`java -version`).
2. **MySQL Database**: Server running on `localhost:3306`.
3. **Database Setup**: Execute the following SQL to prepare the databases:
   ```sql
   CREATE DATABASE IF NOT EXISTS inventory_db;
   CREATE DATABASE IF NOT EXISTS wallet_db;
   CREATE DATABASE IF NOT EXISTS shop_db;
   ```

### Startup Sequence

To ensure configuration and discovery resolve correctly, start services in the following order (each in a separate terminal window):

#### Step 1: Config Server (Port 8888)
```bash
./mvnw -pl config-server spring-boot:run
```

#### Step 2: Eureka Server (Port 8761)
```bash
./mvnw -pl eureka-server spring-boot:run
```
*Verification*: Open [http://localhost:8761](http://localhost:8761) in a browser to inspect the Eureka Dashboard.

#### Step 3: Domain Microservices
Start the three business services:

1. **Inventory Service** (Port 8081):
   ```bash
   ./mvnw -pl inventory-service spring-boot:run
   ```
2. **Wallet Service** (Port 8082):
   ```bash
   ./mvnw -pl wallet-service spring-boot:run
   ```
3. **Shop Service** (Port 8083):
   ```bash
   ./mvnw -pl shop-service spring-boot:run
   ```

#### Step 4: API Gateway (Port 8080)
```bash
./mvnw -pl api-gateway spring-boot:run
```

### Verification & Testing
- **Eureka Dashboard**: Visit [http://localhost:8761](http://localhost:8761) to verify `INVENTORY-SERVICE`, `WALLET-SERVICE`, `SHOP-SERVICE`, and `API-GATEWAY` are registered and status is `UP`.
- **API Entry Point (Port 8080)**:
  - `POST http://localhost:8080/api/v1/auth/login` -> Authenticate & get JWT.
  - `GET http://localhost:8080/api/v1/products` -> Fetch product catalog.
  - `POST http://localhost:8080/api/v1/orders` -> Trigger checkout saga (`Authorization: Bearer <JWT>`).

---

## 3. Scaling Services Horizontally

To run additional instances of an existing service for high availability or load balancing:

1. **Launch a second instance on a different port**:
   ```bash
   ./mvnw -pl shop-service spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"
   ```
2. **Automatic Load Balancing**:
   - Both instances (`8083` and `8084`) register under `SHOP-SERVICE` in Eureka.
   - Spring Cloud LoadBalancer (`lb://SHOP-SERVICE`) in `api-gateway` and internal Feign clients automatically balances traffic between both instances.

---

## 4. How to Add a Brand New Microservice

To add a new microservice (e.g., `notification-service`):

### Step 1: Declare Submodule in Root `pom.xml`
In root [pom.xml](file:///d:/Projects/SprintBoot/ejada-final-project/pom.xml), add the new module:
```xml
<modules>
    ...
    <module>notification-service</module>
</modules>
```

### Step 2: Configure `notification-service/pom.xml`
Create `notification-service/pom.xml` inheriting from root parent:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ejada</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>notification-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

### Step 3: Add Service Configuration in `config-server`
Create `config-server/src/main/resources/config/notification-service.yml`:
```yaml
server:
  port: 8085

spring:
  application:
    name: notification-service

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka}
  instance:
    prefer-ip-address: true
```

### Step 4: Add Application Class & Bootstrap Config
Create `notification-service/src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: notification-service
  config:
    import: "optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
```

Create `notification-service/src/main/java/com/ejada/ecommerce/notification/NotificationApplication.java`:
```java
package com.ejada.ecommerce.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
```

### Step 5: Configure Gateway Routing
In `config-server/src/main/resources/config/api-gateway.yml`, add route predicate:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: notification-service
          uri: lb://NOTIFICATION-SERVICE
          predicates:
            - Path=/api/v1/notifications/**
```

### Step 6: Create Inter-Service Feign Client (Optional)
If `shop-service` needs to communicate with `notification-service`:
```java
@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/v1/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);
}
```
