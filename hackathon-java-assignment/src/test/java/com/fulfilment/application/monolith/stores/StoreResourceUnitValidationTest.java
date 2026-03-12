package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

public class StoreResourceUnitValidationTest {

  @Test
  public void createWithIdReturns422() {
    StoreResource resource = new StoreResource();
    Store s = new Store();
    s.id = 1L;
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.create(s));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  public void updateWithoutNameReturns422() {
    StoreResource resource = new StoreResource();
    Store s = new Store();
    s.name = null;
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.update(1L, s));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  public void patchWithoutNameReturns422() {
    StoreResource resource = new StoreResource();
    Store s = new Store();
    s.name = null;
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.patch(1L, s));
    assertEquals(422, ex.getResponse().getStatus());
  }
}

