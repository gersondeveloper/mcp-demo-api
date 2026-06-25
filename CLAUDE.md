# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Dev mode with live reload (starts Dev Services — auto-spins up PostgreSQL via Testcontainers)
./mvnw quarkus:dev

# Run unit tests (requires Dev Services / running DB)
./mvnw test

# Run a single test class
./mvnw test -Dtest=GreetingResourceTest

# Run integration tests against a packaged JAR
./mvnw verify -DskipITs=false

# Package as JVM JAR
./mvnw package

# Build native executable (requires GraalVM)
./mvnw package -Dnative

# Build native executable without local GraalVM (uses container build)
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## Architecture

**Stack:** Quarkus 3.36.3, Java 25, Hibernate ORM (JPA), Quarkus REST (Jakarta REST / JAX-RS), PostgreSQL (JDBC).

**Package root:** `org.gersondeveloper`

The app is a standard Quarkus JVM/native REST API backed by PostgreSQL:

- REST resources live in the root package and are annotated with `@Path`. CDI injection (`@Inject`) wires dependencies.
- JPA entities use `jakarta.persistence` annotations. `EntityManager` is injected for persistence operations.
- `src/main/resources/import.sql` is executed automatically in **dev and test** modes to seed data. Edit this file to add fixture rows.
- `src/main/resources/application.properties` holds runtime config (datasource URL, Hibernate DDL-auto, etc.). In dev mode Quarkus Dev Services provides a throwaway PostgreSQL container — no local DB setup is required.

**Test layers:**
- `@QuarkusTest` (e.g. `GreetingResourceTest`) — runs against a live Quarkus instance with Dev Services.
- `@QuarkusIntegrationTest` (e.g. `GreetingResourceIT`) — runs the same tests against the packaged artifact; enabled via `-DskipITs=false`.

**Dev UI** is available at `http://localhost:8080/q/dev/` while `quarkus:dev` is running.

## Structure

**Domain**

    - Models:
        - User
        - Product
        - Order
        - Order Item

**Endpoints**

**User** endpoint

    - `GET /api/users/{id}`: Retrieve user by id 
    - `GET /api/users`:  Retrieve all active users
    - `PUT /api/users/{id}`: updates user
    - `POST /api/users`: creates a new user 

**Product** endpoint

    - `GET /api/products/{id}`: Retrieve product by id 
    - `GET /api/products`:  Retrieve all active products
    - `PUT /api/products/{id}`: updates product
    - `POST /api/products`: creates a new product 

**Order** endpoint

    - `GET /api/orders/{id}`: Retrieve order by id 
    - `GET /api/orders`:  Retrieve all active orders
    - `PUT /api/orders/{id}`: updates order
    - `POST /api/orders`: creates a new order 

**Order Item** endpoint

    - `GET /api/orderItems/{id}`: Retrieve order item by id 
    - `GET /api/orderItems/`:  Retrieve all active order items
    - `PUT /api/orderItems/{id}`: updates order item
    - `POST /api/orderItems/`: creates a new order item 

To maintain consistency, all entities must have the following fields:

    - createDate
    - isActive
    - modificationDate
    

