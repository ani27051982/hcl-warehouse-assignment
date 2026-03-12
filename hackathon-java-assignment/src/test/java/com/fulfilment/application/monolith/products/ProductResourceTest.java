package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductResourceTest {

  @Test
  @Transactional
  public void testListAndCreateProduct() {
    // list existing products
    given().when().get("/product").then().statusCode(200);

    // create a new product
    String body =
        """
        {
          "name": "TEST-PRODUCT",
          "description": "Test description",
          "price": 9.99,
          "stock": 5
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("name", equalTo("TEST-PRODUCT"));
  }

  @Test
  public void testGetNonExistingProductReturns404() {
    given().when().get("/product/99999").then().statusCode(404);
  }

  @Test
  public void testCreateWithIdReturns422() {
    String body =
        """
        {
          "id": 1,
          "name": "INVALID-ID",
          "stock": 1
        }
        """;

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("/product")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateAndDeleteProduct() {
    String createBody =
        """
        {
          "name": "UPD-PRODUCT",
          "description": "d1",
          "price": 1.00,
          "stock": 1
        }
        """;

    int id =
        given()
            .contentType("application/json")
            .body(createBody)
            .when()
            .post("/product")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String updateBody =
        """
        {
          "name": "UPD-PRODUCT-2",
          "description": "d2",
          "price": 2.00,
          "stock": 3
        }
        """;

    given()
        .contentType("application/json")
        .body(updateBody)
        .when()
        .put("/product/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("UPD-PRODUCT-2"))
        .body("stock", equalTo(3));

    given().when().delete("/product/" + id).then().statusCode(204);
    given().when().get("/product/" + id).then().statusCode(404);
  }

  @Test
  public void testUpdateValidationAndNotFoundPaths() {
    // update without name -> 422
    String bodyMissingName =
        """
        {
          "description": "x",
          "price": 1.00,
          "stock": 1
        }
        """;
    given()
        .contentType("application/json")
        .body(bodyMissingName)
        .when()
        .put("/product/1")
        .then()
        .statusCode(422);

    // update non-existing -> 404
    String validBody =
        """
        {
          "name": "X",
          "description": "x",
          "price": 1.00,
          "stock": 1
        }
        """;
    given()
        .contentType("application/json")
        .body(validBody)
        .when()
        .put("/product/99999")
        .then()
        .statusCode(404);

    // delete non-existing -> 404
    given().when().delete("/product/99999").then().statusCode(404);
  }
}

