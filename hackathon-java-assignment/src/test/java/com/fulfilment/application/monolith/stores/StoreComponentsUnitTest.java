package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

public class StoreComponentsUnitTest {

  @Test
  public void eventObserverCallsLegacyGateway() {
    LegacyStoreManagerGateway legacy = mock(LegacyStoreManagerGateway.class);

    StoreEventObserver observer = new StoreEventObserver();
    observer.legacyStoreManagerGateway = legacy;

    Store store = new Store();
    store.name = "S1";
    store.quantityProductsInStock = 1;

    observer.onStoreCreated(new StoreCreatedEvent(store));
    observer.onStoreUpdated(new StoreUpdatedEvent(store));

    verify(legacy, times(1)).createStoreOnLegacySystem(any(Store.class));
    verify(legacy, times(1)).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void legacyGatewayWritesTempFileWithoutThrowing() {
    LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();
    Store store = new Store();
    store.name = "TEMPFILE_TEST";
    store.quantityProductsInStock = 3;
    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  public void errorMapperReturnsJsonForWebApplicationException() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    var response = mapper.toResponse(new WebApplicationException("bad", 422));
    assertEquals(422, response.getStatus());
    assertNotNull(response.getEntity());
  }

  @Test
  public void errorMapperReturnsJsonForRuntimeException() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    var response = mapper.toResponse(new RuntimeException("boom"));
    assertEquals(500, response.getStatus());
    assertNotNull(response.getEntity());
  }
}

