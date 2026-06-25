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
