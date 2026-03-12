package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseApiResourceTest {

  @Test
  public void listAndGetWarehouse() {
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));

    given()
        .when()
        .get("/warehouse/MWH.001")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"));
  }

  @Test
  public void createWarehouseValidationAndHappyPath() {
    // invalid location -> 400
    String invalid =
        """
        {
          "businessUnitCode": "NEW-001",
          "location": "NOPE-001",
          "capacity": 10,
          "stock": 1
        }
        """;

    given()
        .contentType("application/json")
        .body(invalid)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);

    // valid create -> 200 (generated interface uses 200/JSON for create response)
    String valid =
        """
        {
          "businessUnitCode": "NEW-002",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 1
        }
        """;

    given()
        .contentType("application/json")
        .body(valid)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("businessUnitCode", equalTo("NEW-002"));

    given().when().get("/warehouse/NEW-002").then().statusCode(200);
  }

  @Test
  public void archiveAndReplaceWarehouse() {
    // Create a dedicated warehouse for archive testing so tests remain isolated.
    String createArchive =
        """
        {
          "businessUnitCode": "ARCH-IT-001",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 1
        }
        """;

    given()
        .contentType("application/json")
        .body(createArchive)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(anyOf(is(200), is(201)));

    // archive existing -> 204/200 depending on generated implementation
    given().when().delete("/warehouse/ARCH-IT-001").then().statusCode(anyOf(is(204), is(200)));

    // archive again -> 400
    given().when().delete("/warehouse/ARCH-IT-001").then().statusCode(400);

    // replace archived -> 400
    String replacement =
        """
        {
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 1
        }
        """;

    given()
        .contentType("application/json")
        .body(replacement)
        .when()
        .post("/warehouse/ARCH-IT-001/replacement")
        .then()
        .statusCode(400);

    // Create a dedicated warehouse for replacement testing.
    String createReplace =
        """
        {
          "businessUnitCode": "REP-IT-001",
          "location": "AMSTERDAM-001",
          "capacity": 10,
          "stock": 1
        }
        """;

    given()
        .contentType("application/json")
        .body(createReplace)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(anyOf(is(200), is(201)));

    // replace active -> 200
    given()
        .contentType("application/json")
        .body(replacement)
        .when()
        .post("/warehouse/REP-IT-001/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("REP-IT-001"));
  }
}

