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
