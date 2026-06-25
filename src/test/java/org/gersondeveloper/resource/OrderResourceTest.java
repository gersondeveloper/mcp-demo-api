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
