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

    @Test
    void createBatch_returnsAllCreated() {
        String suffix = String.valueOf(System.nanoTime());
        given()
            .contentType(ContentType.JSON)
            .body("""
                [
                  {"username": "batch_a_%1$s", "address": "Addr A"},
                  {"username": "batch_b_%1$s", "address": "Addr B"},
                  {"username": "batch_c_%1$s", "address": "Addr C"}
                ]
                """.formatted(suffix))
            .when().post("/api/users/batch")
            .then()
            .statusCode(201)
            .body("size()", equalTo(3))
            .body("[0].id", notNullValue())
            .body("[0].username", equalTo("batch_a_" + suffix))
            .body("[2].username", equalTo("batch_c_" + suffix));
    }

    @Test
    void createBatch_emptyList_returns400() {
        given()
            .contentType(ContentType.JSON)
            .body("[]")
            .when().post("/api/users/batch")
            .then()
            .statusCode(400);
    }

    @Test
    void createBatch_duplicateUsername_rollsBackEntireBatch() {
        String unique = "rollback_marker_" + System.nanoTime();
        given()
            .contentType(ContentType.JSON)
            .body("""
                [
                  {"username": "%1$s", "address": "Addr A"},
                  {"username": "%1$s", "address": "Addr B"}
                ]
                """.formatted(unique))
            .when().post("/api/users/batch")
            .then()
            .statusCode(greaterThanOrEqualTo(400));

        given()
            .when().get("/api/users")
            .then()
            .statusCode(200)
            .body("findAll { it.username == '" + unique + "' }.size()", equalTo(0));
    }
}
