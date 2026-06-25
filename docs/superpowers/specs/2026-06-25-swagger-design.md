---
name: swagger-openapi-integration
description: Add Swagger UI and OpenAPI spec to mcp-demo-api via quarkus-smallrye-openapi
metadata:
  type: project
---

# Swagger / OpenAPI Integration

## Goal

Make the mcp-demo-api self-documenting and testable via a browser-based Swagger UI, available in all environments (dev and production).

## Approach

Use the `quarkus-smallrye-openapi` extension, which integrates MicroProfile OpenAPI with Quarkus. The spec is generated automatically from existing JAX-RS annotations — no changes to Java source files are required.

## Changes

### 1. `pom.xml`

Add the following dependency inside `<dependencies>`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-openapi</artifactId>
</dependency>
```

### 2. `src/main/resources/application.yml`

Add the following under the `quarkus:` block:

```yaml
quarkus:
  swagger-ui:
    always-include: true
```

## Endpoints Exposed

After the change, the following URLs are available at runtime:

| URL | Description |
|-----|-------------|
| `http://localhost:8080/q/swagger-ui` | Swagger UI (interactive docs + manual testing) |
| `http://localhost:8080/q/openapi` | Raw OpenAPI spec (JSON/YAML) |

## Resources Documented Automatically

All 4 existing REST resources are picked up with no code changes:

- `UserResource` — `/api/users`
- `ProductResource` — `/api/products`
- `OrderResource` — `/api/orders`
- `OrderItemResource` — `/api/orderItems`

## Out of Scope

- OpenAPI annotations (`@Operation`, `@Tag`, `@APIResponse`, etc.) — not needed for this goal.
- Custom API title, description, or version — default Quarkus values are acceptable.
- Path customization for the Swagger UI URL.
