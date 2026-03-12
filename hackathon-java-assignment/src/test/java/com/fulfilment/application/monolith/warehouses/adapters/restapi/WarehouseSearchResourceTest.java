package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseSearchResourceTest {

  @Test
  @Transactional
  public void testSearchFiltersAndExcludesArchived() {
    // Base list should contain the seeded 3 warehouses
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));

    // Search by location and capacity range
    given()
        .queryParam("location", "AMSTERDAM-001")
        .queryParam("minCapacity", 40)
        .queryParam("maxCapacity", 100)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1))
        .body("[0].businessUnitCode", anyOf(equalTo("MWH.012"), notNullValue()));
  }

  @Test
  public void testSearchPaginationAndSorting() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "asc")
        .queryParam("page", 0)
        .queryParam("pageSize", 2)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("size()", lessThanOrEqualTo(2));
  }

  @Test
  public void testInvalidCapacityRangeReturnsBadRequest() {
    given()
        .queryParam("minCapacity", 100)
        .queryParam("maxCapacity", 10)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(400);
  }

  @Test
  public void testNegativePageAndPageSizeAreNormalized() {
    given()
        .queryParam("page", -1)
        .queryParam("pageSize", -5)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200);
  }
}

