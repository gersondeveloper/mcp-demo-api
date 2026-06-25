# Swagger / OpenAPI Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Swagger UI and OpenAPI spec to mcp-demo-api, available in all environments.

**Architecture:** Add the `quarkus-smallrye-openapi` extension to the Maven build; configure `always-include: true` so the Swagger UI is bundled in production as well as dev mode. No Java source changes are needed — the spec is generated automatically from existing JAX-RS annotations.

**Tech Stack:** Quarkus 3.36.3, quarkus-smallrye-openapi (MicroProfile OpenAPI), Maven

## Global Constraints

- Quarkus version: 3.36.3 (managed via BOM — do NOT specify a version for `quarkus-smallrye-openapi`)
- Java 21 (maven.compiler.release = 21)
- No Java source file modifications
- No OpenAPI annotations to be added

---

### Task 1: Add quarkus-smallrye-openapi dependency and enable Swagger UI in all environments

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Produces: Swagger UI at `http://localhost:8080/q/swagger-ui`, OpenAPI spec at `http://localhost:8080/q/openapi`

- [ ] **Step 1: Add dependency to pom.xml**

In `pom.xml`, inside the `<dependencies>` block, add after the last existing `<dependency>` entry:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-openapi</artifactId>
</dependency>
```

- [ ] **Step 2: Enable Swagger UI in production builds**

In `src/main/resources/application.yml`, add `swagger-ui.always-include: true` under the existing `quarkus:` root key. The file should look like this after the change:

```yaml
quarkus:
  datasource:
    db-kind: postgresql
    username: user-test
    password: "user_123#"
    jdbc:
      url: jdbc:postgresql://localhost:5432/mcp-demo-api

  hibernate-orm:
    database:
      generation: update

  swagger-ui:
    always-include: true

"%dev":
  quarkus:
    hibernate-orm:
      log:
        sql: true
```

- [ ] **Step 3: Start the app in dev mode and verify**

Run:
```bash
./mvnw quarkus:dev
```

Open `http://localhost:8080/q/swagger-ui` in a browser.

Expected: Swagger UI loads and shows 4 endpoint groups — `/api/users`, `/api/products`, `/api/orders`, `/api/orderItems`.

Also verify the raw spec:
```bash
curl http://localhost:8080/q/openapi
```
Expected: YAML output listing all endpoints.

- [ ] **Step 4: Commit**

```bash
git add pom.xml src/main/resources/application.yml
git commit -m "feat: add Swagger UI via quarkus-smallrye-openapi"
```
