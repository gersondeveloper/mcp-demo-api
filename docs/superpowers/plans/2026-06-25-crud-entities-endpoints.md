# CRUD Entities & Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement full CRUD for User, Product, Order, and OrderItem with Panache Active Record, Java records as DTOs, and JAX-RS endpoints.

**Architecture:** Four JPA entities extend a shared `BaseEntity` (which extends `PanacheEntity`) for common audit fields. Each entity has a pair of Java records (request/response) and a JAX-RS resource class. Order creation auto-calculates `totalAmount` and sets `unitPrice` from product price at time of creation.

**Tech Stack:** Quarkus 3.36.3, Java 21, Panache Active Record, quarkus-rest + quarkus-rest-jackson, PostgreSQL (Dev Services in dev/test).

## Global Constraints

- Package root: `org.gersondeveloper`
- All entities extend `BaseEntity`, which extends `PanacheEntity`
- All entities must have `createDate`, `modificationDate` (auto-managed via JPA lifecycle callbacks), `isActive` (default `true`)
- All GET list endpoints return only records where `isActive = true`
- DTOs are Java records — no classes
- No soft-delete endpoints; `isActive` field is managed internally
- JSON serialization via Jackson (`quarkus-rest-jackson`)
- Tests: `@QuarkusTest` + Rest Assured against live Quarkus with Dev Services

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `pom.xml` | Modify | Add `quarkus-rest-jackson` |
| `domain/model/BaseEntity.java` | Create | Common audit fields + JPA lifecycle callbacks |
| `domain/enums/OrderStatus.java` | Create | PENDING, CONFIRMED, CANCELLED |
| `domain/model/User.java` | Modify | Extend `BaseEntity` |
| `domain/model/Product.java` | Create | Product entity |
| `domain/model/Order.java` | Create | Order entity with `ManyToOne User` and `OneToMany OrderItem` |
| `domain/model/OrderItem.java` | Create | OrderItem entity with `ManyToOne Order` and `ManyToOne Product` |
| `domain/dto/request/UserRequest.java` | Create | Record for User POST/PUT body |
| `domain/dto/response/UserResponse.java` | Create | Record for User GET response |
| `domain/dto/request/ProductRequest.java` | Create | Record for Product POST/PUT body |
| `domain/dto/response/ProductResponse.java` | Create | Record for Product GET response |
| `domain/dto/request/OrderRequest.java` | Create | Record for Order POST/PUT body |
| `domain/dto/request/OrderItemRequest.java` | Create | Record for embedded item inside OrderRequest |
| `domain/dto/request/StandaloneOrderItemRequest.java` | Create | Record for OrderItem standalone POST (includes orderId) |
| `domain/dto/response/OrderResponse.java` | Create | Record for Order GET response |
| `domain/dto/response/OrderItemResponse.java` | Create | Record for OrderItem inside OrderResponse |
| `resource/UserResource.java` | Create | GET list, GET by id, POST, PUT for `/api/users` |
| `resource/ProductResource.java` | Create | GET list, GET by id, POST, PUT for `/api/products` |
| `resource/OrderResource.java` | Create | GET list, GET by id, POST, PUT for `/api/orders` |
| `resource/OrderItemResource.java` | Create | GET list, GET by id, POST, PUT for `/api/orderItems` |
| `test/.../UserResourceTest.java` | Create | QuarkusTest for User endpoints |
| `test/.../ProductResourceTest.java` | Create | QuarkusTest for Product endpoints |
| `test/.../OrderResourceTest.java` | Create | QuarkusTest for Order endpoints |
| `test/.../OrderItemResourceTest.java` | Create | QuarkusTest for OrderItem endpoints |

---

## Task 1: Foundation — pom.xml + BaseEntity + OrderStatus

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/org/gersondeveloper/domain/model/BaseEntity.java`
- Create: `src/main/java/org/gersondeveloper/domain/enums/OrderStatus.java`

**Interfaces:**
- Produces: `BaseEntity` (extended by all entities), `OrderStatus` (used by `Order`)

- [ ] **Step 1: Add quarkus-rest-jackson to pom.xml**

In `pom.xml`, add after the `quarkus-hibernate-orm-panache` dependency:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

- [ ] **Step 2: Create BaseEntity**

Create `src/main/java/org/gersondeveloper/domain/model/BaseEntity.java`:

```java
package org.gersondeveloper.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity extends PanacheEntity {

    @Column(nullable = false, updatable = false)
    public LocalDateTime createDate;

    @Column(nullable = false)
    public LocalDateTime modificationDate;

    @Column(nullable = false)
    public boolean isActive = true;

    @PrePersist
    void onCreate() {
        createDate = LocalDateTime.now();
        modificationDate = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        modificationDate = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Create OrderStatus enum**

Create `src/main/java/org/gersondeveloper/domain/enums/OrderStatus.java`:

```java
package org.gersondeveloper.domain.enums;

public enum OrderStatus {
    PENDING, CONFIRMED, CANCELLED
}
```

- [ ] **Step 4: Verify compilation**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS with no errors.

- [ ] **Step 5: Commit**

```bash
git add pom.xml \
  src/main/java/org/gersondeveloper/domain/model/BaseEntity.java \
  src/main/java/org/gersondeveloper/domain/enums/OrderStatus.java
git commit -m "feat: add BaseEntity, OrderStatus enum, and quarkus-rest-jackson"
```

---

## Task 2: User — entity, DTOs, Resource + Tests

**Files:**
- Modify: `src/main/java/org/gersondeveloper/domain/model/User.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/request/UserRequest.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/response/UserResponse.java`
- Create: `src/main/java/org/gersondeveloper/resource/UserResource.java`
- Create: `src/test/java/org/gersondeveloper/resource/UserResourceTest.java`

**Interfaces:**
- Consumes: `BaseEntity` from Task 1
- Produces: `UserResource` at `/api/users`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/gersondeveloper/resource/UserResourceTest.java`:

```java
package org.gersondeveloper.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserResourceTest {

    @Test
    void createUser_returnsCreated() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "alice", "address": "123 Main St"}
                """)
            .when().post("/api/users")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("username", equalTo("alice"))
            .body("address", equalTo("123 Main St"))
            .body("isActive", equalTo(true))
            .body("createDate", notNullValue());
    }

    @Test
    void listActiveUsers_returns200() {
        given()
            .when().get("/api/users")
            .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void findById_existingUser_returnsUser() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "bob_find", "address": "456 Oak Ave"}
                """)
            .when().post("/api/users")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .when().get("/api/users/" + id)
            .then()
            .statusCode(200)
            .body("id", equalTo(id))
            .body("username", equalTo("bob_find"));
    }

    @Test
    void findById_unknownId_returns404() {
        given()
            .when().get("/api/users/999999")
            .then()
            .statusCode(404);
    }

    @Test
    void updateUser_existingUser_returnsUpdated() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "carol_update", "address": "789 Pine Rd"}
                """)
            .when().post("/api/users")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "carol_updated", "address": "999 New Rd"}
                """)
            .when().put("/api/users/" + id)
            .then()
            .statusCode(200)
            .body("username", equalTo("carol_updated"))
            .body("address", equalTo("999 New Rd"));
    }

    @Test
    void updateUser_unknownId_returns404() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "ghost", "address": "nowhere"}
                """)
            .when().put("/api/users/999999")
            .then()
            .statusCode(404);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=UserResourceTest -q 2>&1 | tail -20
```

Expected: FAIL — `UserResource` does not exist yet.

- [ ] **Step 3: Update User entity to extend BaseEntity**

Replace the content of `src/main/java/org/gersondeveloper/domain/model/User.java`:

```java
package org.gersondeveloper.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    public String username;

    @Column(nullable = false)
    public String address;
}
```

- [ ] **Step 4: Create UserRequest record**

Create `src/main/java/org/gersondeveloper/domain/dto/request/UserRequest.java`:

```java
package org.gersondeveloper.domain.dto.request;

public record UserRequest(String username, String address) {}
```

- [ ] **Step 5: Create UserResponse record**

Create `src/main/java/org/gersondeveloper/domain/dto/response/UserResponse.java`:

```java
package org.gersondeveloper.domain.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String address,
        boolean isActive,
        LocalDateTime createDate) {}
```

- [ ] **Step 6: Create UserResource**

Create `src/main/java/org/gersondeveloper/resource/UserResource.java`:

```java
package org.gersondeveloper.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gersondeveloper.domain.dto.request.UserRequest;
import org.gersondeveloper.domain.dto.response.UserResponse;
import org.gersondeveloper.domain.model.User;

import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public List<UserResponse> listActive() {
        return User.<User>list("isActive", true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        User user = User.findById(id);
        if (user == null || !user.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toResponse(user)).build();
    }

    @POST
    @Transactional
    public Response create(UserRequest request) {
        User user = new User();
        user.username = request.username();
        user.address = request.address();
        user.persist();
        return Response.status(Response.Status.CREATED).entity(toResponse(user)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, UserRequest request) {
        User user = User.findById(id);
        if (user == null || !user.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        user.username = request.username();
        user.address = request.address();
        return Response.ok(toResponse(user)).build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.id, user.username, user.address, user.isActive, user.createDate);
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
./mvnw test -Dtest=UserResourceTest -q 2>&1 | tail -20
```

Expected: BUILD SUCCESS, all 6 tests pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/org/gersondeveloper/domain/model/User.java \
  src/main/java/org/gersondeveloper/domain/dto/request/UserRequest.java \
  src/main/java/org/gersondeveloper/domain/dto/response/UserResponse.java \
  src/main/java/org/gersondeveloper/resource/UserResource.java \
  src/test/java/org/gersondeveloper/resource/UserResourceTest.java
git commit -m "feat: add User entity, DTOs, and CRUD endpoint"
```

---

## Task 3: Product — entity, DTOs, Resource + Tests

**Files:**
- Create: `src/main/java/org/gersondeveloper/domain/model/Product.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/request/ProductRequest.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/response/ProductResponse.java`
- Create: `src/main/java/org/gersondeveloper/resource/ProductResource.java`
- Create: `src/test/java/org/gersondeveloper/resource/ProductResourceTest.java`

**Interfaces:**
- Consumes: `BaseEntity` from Task 1
- Produces: `ProductResource` at `/api/products`; `Product` entity (used by `OrderResource` in Task 4)

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/gersondeveloper/resource/ProductResourceTest.java`:

```java
package org.gersondeveloper.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ProductResourceTest {

    @Test
    void createProduct_returnsCreated() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Laptop", "description": "A fast laptop", "price": 1500.00, "stockQuantity": 10}
                """)
            .when().post("/api/products")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Laptop"))
            .body("price", equalTo(1500.0f))
            .body("stockQuantity", equalTo(10))
            .body("isActive", equalTo(true));
    }

    @Test
    void listActiveProducts_returns200() {
        given()
            .when().get("/api/products")
            .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void findById_existingProduct_returnsProduct() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Mouse", "description": "Wireless mouse", "price": 29.99, "stockQuantity": 50}
                """)
            .when().post("/api/products")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .when().get("/api/products/" + id)
            .then()
            .statusCode(200)
            .body("id", equalTo(id))
            .body("name", equalTo("Mouse"));
    }

    @Test
    void findById_unknownId_returns404() {
        given()
            .when().get("/api/products/999999")
            .then()
            .statusCode(404);
    }

    @Test
    void updateProduct_existingProduct_returnsUpdated() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Keyboard", "description": "Mechanical", "price": 89.99, "stockQuantity": 20}
                """)
            .when().post("/api/products")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Keyboard Pro", "description": "Mechanical RGB", "price": 129.99, "stockQuantity": 15}
                """)
            .when().put("/api/products/" + id)
            .then()
            .statusCode(200)
            .body("name", equalTo("Keyboard Pro"))
            .body("price", equalTo(129.99f));
    }

    @Test
    void updateProduct_unknownId_returns404() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Ghost", "description": "N/A", "price": 1.00, "stockQuantity": 0}
                """)
            .when().put("/api/products/999999")
            .then()
            .statusCode(404);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=ProductResourceTest -q 2>&1 | tail -20
```

Expected: FAIL — `ProductResource` does not exist yet.

- [ ] **Step 3: Create Product entity**

Create `src/main/java/org/gersondeveloper/domain/model/Product.java`:

```java
package org.gersondeveloper.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    public String name;

    public String description;

    @Column(nullable = false)
    public BigDecimal price;

    @Column(nullable = false)
    public int stockQuantity;
}
```

- [ ] **Step 4: Create ProductRequest record**

Create `src/main/java/org/gersondeveloper/domain/dto/request/ProductRequest.java`:

```java
package org.gersondeveloper.domain.dto.request;

import java.math.BigDecimal;

public record ProductRequest(String name, String description, BigDecimal price, int stockQuantity) {}
```

- [ ] **Step 5: Create ProductResponse record**

Create `src/main/java/org/gersondeveloper/domain/dto/response/ProductResponse.java`:

```java
package org.gersondeveloper.domain.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean isActive) {}
```

- [ ] **Step 6: Create ProductResource**

Create `src/main/java/org/gersondeveloper/resource/ProductResource.java`:

```java
package org.gersondeveloper.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gersondeveloper.domain.dto.request.ProductRequest;
import org.gersondeveloper.domain.dto.response.ProductResponse;
import org.gersondeveloper.domain.model.Product;

import java.util.List;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @GET
    public List<ProductResponse> listActive() {
        return Product.<Product>list("isActive", true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        Product product = Product.findById(id);
        if (product == null || !product.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toResponse(product)).build();
    }

    @POST
    @Transactional
    public Response create(ProductRequest request) {
        Product product = new Product();
        product.name = request.name();
        product.description = request.description();
        product.price = request.price();
        product.stockQuantity = request.stockQuantity();
        product.persist();
        return Response.status(Response.Status.CREATED).entity(toResponse(product)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, ProductRequest request) {
        Product product = Product.findById(id);
        if (product == null || !product.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        product.name = request.name();
        product.description = request.description();
        product.price = request.price();
        product.stockQuantity = request.stockQuantity();
        return Response.ok(toResponse(product)).build();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id, product.name, product.description,
                product.price, product.stockQuantity, product.isActive);
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
./mvnw test -Dtest=ProductResourceTest -q 2>&1 | tail -20
```

Expected: BUILD SUCCESS, all 6 tests pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/org/gersondeveloper/domain/model/Product.java \
  src/main/java/org/gersondeveloper/domain/dto/request/ProductRequest.java \
  src/main/java/org/gersondeveloper/domain/dto/response/ProductResponse.java \
  src/main/java/org/gersondeveloper/resource/ProductResource.java \
  src/test/java/org/gersondeveloper/resource/ProductResourceTest.java
git commit -m "feat: add Product entity, DTOs, and CRUD endpoint"
```

---

## Task 4: Order + OrderItem — entities, DTOs, Resources + Tests

**Files:**
- Create: `src/main/java/org/gersondeveloper/domain/model/Order.java`
- Create: `src/main/java/org/gersondeveloper/domain/model/OrderItem.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/request/OrderRequest.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/request/OrderItemRequest.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/request/StandaloneOrderItemRequest.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/response/OrderResponse.java`
- Create: `src/main/java/org/gersondeveloper/domain/dto/response/OrderItemResponse.java`
- Create: `src/main/java/org/gersondeveloper/resource/OrderResource.java`
- Create: `src/main/java/org/gersondeveloper/resource/OrderItemResource.java`
- Create: `src/test/java/org/gersondeveloper/resource/OrderResourceTest.java`
- Create: `src/test/java/org/gersondeveloper/resource/OrderItemResourceTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 1), `OrderStatus` (Task 1), `User` (Task 2), `Product` (Task 3)
- Produces: `OrderResource` at `/api/orders`, `OrderItemResource` at `/api/orderItems`

- [ ] **Step 1: Write the failing Order test**

Create `src/test/java/org/gersondeveloper/resource/OrderResourceTest.java`:

```java
package org.gersondeveloper.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class OrderResourceTest {

    private int userId;
    private int productId;

    @BeforeEach
    void setUp() {
        userId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "order_user_%d", "address": "1 Order St"}
                """.formatted(System.nanoTime()))
            .when().post("/api/users")
            .then().statusCode(201)
            .extract().path("id");

        productId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Widget", "description": "A widget", "price": 10.00, "stockQuantity": 100}
                """)
            .when().post("/api/products")
            .then().statusCode(201)
            .extract().path("id");
    }

    @Test
    void createOrder_returnsCreated() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "userId": %d,
                  "items": [{"productId": %d, "quantity": 2}]
                }
                """.formatted(userId, productId))
            .when().post("/api/orders")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("status", equalTo("PENDING"))
            .body("totalAmount", equalTo(20.0f))
            .body("userId", equalTo(userId))
            .body("items", hasSize(1))
            .body("items[0].quantity", equalTo(2))
            .body("items[0].unitPrice", equalTo(10.0f));
    }

    @Test
    void listActiveOrders_returns200() {
        given()
            .when().get("/api/orders")
            .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void findById_existingOrder_returnsOrder() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "userId": %d,
                  "items": [{"productId": %d, "quantity": 1}]
                }
                """.formatted(userId, productId))
            .when().post("/api/orders")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .when().get("/api/orders/" + id)
            .then()
            .statusCode(200)
            .body("id", equalTo(id));
    }

    @Test
    void findById_unknownId_returns404() {
        given()
            .when().get("/api/orders/999999")
            .then()
            .statusCode(404);
    }

    @Test
    void updateOrder_existingOrder_returnsUpdated() {
        int id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "userId": %d,
                  "items": [{"productId": %d, "quantity": 3}]
                }
                """.formatted(userId, productId))
            .when().post("/api/orders")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"userId": %d, "items": []}
                """.formatted(userId))
            .when().put("/api/orders/" + id)
            .then()
            .statusCode(200)
            .body("userId", equalTo(userId));
    }
}
```

- [ ] **Step 2: Write the failing OrderItem test**

Create `src/test/java/org/gersondeveloper/resource/OrderItemResourceTest.java`:

```java
package org.gersondeveloper.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class OrderItemResourceTest {

    private int orderId;
    private int productId;

    @BeforeEach
    void setUp() {
        int userId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "oi_user_%d", "address": "2 Item St"}
                """.formatted(System.nanoTime()))
            .when().post("/api/users")
            .then().statusCode(201)
            .extract().path("id");

        productId = given()
            .contentType(ContentType.JSON)
            .body("""
                {"name": "Gadget", "description": "A gadget", "price": 25.00, "stockQuantity": 50}
                """)
            .when().post("/api/products")
            .then().statusCode(201)
            .extract().path("id");

        orderId = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "userId": %d,
                  "items": [{"productId": %d, "quantity": 1}]
                }
                """.formatted(userId, productId))
            .when().post("/api/orders")
            .then().statusCode(201)
            .extract().path("id");
    }

    @Test
    void listActiveOrderItems_returns200() {
        given()
            .when().get("/api/orderItems")
            .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void findById_existingItem_returnsItem() {
        int itemId = given()
            .when().get("/api/orders/" + orderId)
            .then().statusCode(200)
            .extract().path("items[0].id");

        given()
            .when().get("/api/orderItems/" + itemId)
            .then()
            .statusCode(200)
            .body("id", equalTo(itemId));
    }

    @Test
    void findById_unknownId_returns404() {
        given()
            .when().get("/api/orderItems/999999")
            .then()
            .statusCode(404);
    }

    @Test
    void createStandaloneOrderItem_returnsCreated() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"orderId": %d, "productId": %d, "quantity": 3}
                """.formatted(orderId, productId))
            .when().post("/api/orderItems")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("quantity", equalTo(3))
            .body("unitPrice", equalTo(25.0f));
    }

    @Test
    void updateOrderItem_existingItem_returnsUpdated() {
        int itemId = given()
            .when().get("/api/orders/" + orderId)
            .then().statusCode(200)
            .extract().path("items[0].id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {"productId": %d, "quantity": 5}
                """.formatted(productId))
            .when().put("/api/orderItems/" + itemId)
            .then()
            .statusCode(200)
            .body("quantity", equalTo(5));
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./mvnw test -Dtest="OrderResourceTest,OrderItemResourceTest" -q 2>&1 | tail -20
```

Expected: FAIL — entities and resources do not exist yet.

- [ ] **Step 4: Create Order entity**

Create `src/main/java/org/gersondeveloper/domain/model/Order.java`:

```java
package org.gersondeveloper.domain.model;

import jakarta.persistence.*;
import org.gersondeveloper.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    public User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OrderItem> items = new ArrayList<>();
}
```

- [ ] **Step 5: Create OrderItem entity**

Create `src/main/java/org/gersondeveloper/domain/model/OrderItem.java`:

```java
package org.gersondeveloper.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false)
    public BigDecimal unitPrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    public Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    public Product product;
}
```

- [ ] **Step 6: Create OrderItemRequest record**

Create `src/main/java/org/gersondeveloper/domain/dto/request/OrderItemRequest.java`:

```java
package org.gersondeveloper.domain.dto.request;

public record OrderItemRequest(Long productId, int quantity) {}
```

- [ ] **Step 7: Create OrderRequest record**

Create `src/main/java/org/gersondeveloper/domain/dto/request/OrderRequest.java`:

```java
package org.gersondeveloper.domain.dto.request;

import java.util.List;

public record OrderRequest(Long userId, List<OrderItemRequest> items) {}
```

- [ ] **Step 8: Create StandaloneOrderItemRequest record**

Create `src/main/java/org/gersondeveloper/domain/dto/request/StandaloneOrderItemRequest.java`:

```java
package org.gersondeveloper.domain.dto.request;

public record StandaloneOrderItemRequest(Long orderId, Long productId, int quantity) {}
```

- [ ] **Step 9: Create OrderItemResponse record**

Create `src/main/java/org/gersondeveloper/domain/dto/response/OrderItemResponse.java`:

```java
package org.gersondeveloper.domain.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice) {}
```

- [ ] **Step 10: Create OrderResponse record**

Create `src/main/java/org/gersondeveloper/domain/dto/response/OrderResponse.java`:

```java
package org.gersondeveloper.domain.dto.response;

import org.gersondeveloper.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        Long userId,
        List<OrderItemResponse> items) {}
```

- [ ] **Step 11: Create OrderResource**

Create `src/main/java/org/gersondeveloper/resource/OrderResource.java`:

```java
package org.gersondeveloper.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gersondeveloper.domain.dto.request.OrderItemRequest;
import org.gersondeveloper.domain.dto.request.OrderRequest;
import org.gersondeveloper.domain.dto.response.OrderItemResponse;
import org.gersondeveloper.domain.dto.response.OrderResponse;
import org.gersondeveloper.domain.model.Order;
import org.gersondeveloper.domain.model.OrderItem;
import org.gersondeveloper.domain.model.Product;
import org.gersondeveloper.domain.model.User;

import java.math.BigDecimal;
import java.util.List;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @GET
    public List<OrderResponse> listActive() {
        return Order.<Order>list("isActive", true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        Order order = Order.findById(id);
        if (order == null || !order.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toResponse(order)).build();
    }

    @POST
    @Transactional
    public Response create(OrderRequest request) {
        User user = User.findById(request.userId());
        if (user == null || !user.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Order order = new Order();
        order.user = user;

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.items()) {
            Product product = Product.findById(itemReq.productId());
            if (product == null || !product.isActive) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            OrderItem item = new OrderItem();
            item.product = product;
            item.quantity = itemReq.quantity();
            item.unitPrice = product.price;
            item.order = order;
            order.items.add(item);
            total = total.add(product.price.multiply(BigDecimal.valueOf(itemReq.quantity())));
        }
        order.totalAmount = total;
        order.persist();
        return Response.status(Response.Status.CREATED).entity(toResponse(order)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, OrderRequest request) {
        Order order = Order.findById(id);
        if (order == null || !order.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        User user = User.findById(request.userId());
        if (user == null || !user.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        order.user = user;
        return Response.ok(toResponse(order)).build();
    }

    OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.items.stream()
                .map(item -> new OrderItemResponse(
                        item.id, item.product.id, item.product.name,
                        item.quantity, item.unitPrice))
                .toList();
        return new OrderResponse(order.id, order.status, order.totalAmount, order.user.id, items);
    }
}
```

- [ ] **Step 12: Create OrderItemResource**

Create `src/main/java/org/gersondeveloper/resource/OrderItemResource.java`:

```java
package org.gersondeveloper.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gersondeveloper.domain.dto.request.OrderItemRequest;
import org.gersondeveloper.domain.dto.request.StandaloneOrderItemRequest;
import org.gersondeveloper.domain.dto.response.OrderItemResponse;
import org.gersondeveloper.domain.model.Order;
import org.gersondeveloper.domain.model.OrderItem;
import org.gersondeveloper.domain.model.Product;

import java.util.List;

@Path("/api/orderItems")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderItemResource {

    @GET
    public List<OrderItemResponse> listActive() {
        return OrderItem.<OrderItem>list("isActive", true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        OrderItem item = OrderItem.findById(id);
        if (item == null || !item.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toResponse(item)).build();
    }

    @POST
    @Transactional
    public Response create(StandaloneOrderItemRequest request) {
        Order order = Order.findById(request.orderId());
        if (order == null || !order.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Product product = Product.findById(request.productId());
        if (product == null || !product.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        OrderItem item = new OrderItem();
        item.order = order;
        item.product = product;
        item.quantity = request.quantity();
        item.unitPrice = product.price;
        item.persist();
        return Response.status(Response.Status.CREATED).entity(toResponse(item)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, OrderItemRequest request) {
        OrderItem item = OrderItem.findById(id);
        if (item == null || !item.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Product product = Product.findById(request.productId());
        if (product == null || !product.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        item.product = product;
        item.quantity = request.quantity();
        item.unitPrice = product.price;
        return Response.ok(toResponse(item)).build();
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.id, item.product.id, item.product.name,
                item.quantity, item.unitPrice);
    }
}
```

- [ ] **Step 13: Run all tests**

```bash
./mvnw test -q 2>&1 | tail -30
```

Expected: BUILD SUCCESS — all tests pass including `GreetingResourceTest`.

- [ ] **Step 14: Commit**

```bash
git add \
  src/main/java/org/gersondeveloper/domain/model/Order.java \
  src/main/java/org/gersondeveloper/domain/model/OrderItem.java \
  src/main/java/org/gersondeveloper/domain/dto/request/OrderItemRequest.java \
  src/main/java/org/gersondeveloper/domain/dto/request/OrderRequest.java \
  src/main/java/org/gersondeveloper/domain/dto/request/StandaloneOrderItemRequest.java \
  src/main/java/org/gersondeveloper/domain/dto/response/OrderItemResponse.java \
  src/main/java/org/gersondeveloper/domain/dto/response/OrderResponse.java \
  src/main/java/org/gersondeveloper/resource/OrderResource.java \
  src/main/java/org/gersondeveloper/resource/OrderItemResource.java \
  src/test/java/org/gersondeveloper/resource/OrderResourceTest.java \
  src/test/java/org/gersondeveloper/resource/OrderItemResourceTest.java
git commit -m "feat: add Order and OrderItem entities, DTOs, and CRUD endpoints"
```
