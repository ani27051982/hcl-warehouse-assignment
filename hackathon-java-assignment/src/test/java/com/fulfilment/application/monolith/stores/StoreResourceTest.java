package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  @Test
  @Transactional
  public void testListAndCreateStore() {
    // list existing stores
    given().when().get("/store").then().statusCode(200);

    // create a new store
    String body =
        """
        {
          "name": "TEST-STORE",
          "quantityProductsInStock": 10
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .body("name", equalTo("TEST-STORE"));
  }

  @Test
  public void testGetNonExistingStoreReturns404() {
    given().when().get("/store/99999").then().statusCode(404);
  }

  @Test
  public void testCreateWithIdReturns422() {
    String body =
        """
        {
          "id": 1,
          "name": "INVALID-STORE",
          "quantityProductsInStock": 1
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("/store")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateAndDeleteStore() {
    String createBody =
        """
        {
          "name": "UPD-STORE",
          "quantityProductsInStock": 1
        }
        """;

    int id =
        given()
            .contentType("application/json")
            .body(createBody)
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String updateBody =
        """
        {
          "name": "UPD-STORE-2",
          "quantityProductsInStock": 5
        }
        """;

    given()
        .contentType("application/json")
        .body(updateBody)
        .when()
        .put("/store/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("UPD-STORE-2"))
        .body("quantityProductsInStock", equalTo(5));

    given().when().delete("/store/" + id).then().statusCode(204);
    given().when().get("/store/" + id).then().statusCode(404);
  }

  @Test
  public void testPatchAndValidationPaths() {
    // create
    String createBody =
        """
        {
          "name": "PATCH-STORE",
          "quantityProductsInStock": 2
        }
        """;

    int id =
        given()
            .contentType("application/json")
            .body(createBody)
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    // patch -> 200
    String patchBody =
        """
        {
          "name": "PATCH-STORE-2",
          "quantityProductsInStock": 9
        }
        """;
    given()
        .contentType("application/json")
        .body(patchBody)
        .when()
        .patch("/store/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PATCH-STORE-2"));

    // patch missing name -> 422
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\": 1}")
        .when()
        .patch("/store/" + id)
        .then()
        .statusCode(422);

    // update non-existing -> 404
    given()
        .contentType("application/json")
        .body(patchBody)
        .when()
        .put("/store/99999")
        .then()
        .statusCode(404);

    // delete non-existing -> 404
    given().when().delete("/store/99999").then().statusCode(404);

    // get existing -> 200
    given().when().get("/store/" + id).then().statusCode(200).body("id", equalTo(id));
  }
}

