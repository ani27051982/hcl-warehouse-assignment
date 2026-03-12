package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

public class ProductComponentsUnitTest {

  @Test
  public void errorMapperReturnsJsonForWebApplicationException() {
    ProductResource.ErrorMapper mapper = new ProductResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    var response = mapper.toResponse(new WebApplicationException("bad", 404));
    assertEquals(404, response.getStatus());
    assertNotNull(response.getEntity());
  }

  @Test
  public void errorMapperReturnsJsonForRuntimeException() {
    ProductResource.ErrorMapper mapper = new ProductResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    var response = mapper.toResponse(new RuntimeException("boom"));
    assertEquals(500, response.getStatus());
    assertNotNull(response.getEntity());
  }
}

