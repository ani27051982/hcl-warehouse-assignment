package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class WarehouseResourceImplUnitTest {

  private static void setField(Object target, String fieldName, Object value) {
    try {
      var f = target.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void listAllAndGetByIdHappyPath() {
    WarehouseRepository repo = mock(WarehouseRepository.class);
    CreateWarehouseOperation create = mock(CreateWarehouseOperation.class);
    ArchiveWarehouseOperation archive = mock(ArchiveWarehouseOperation.class);
    ReplaceWarehouseOperation replace = mock(ReplaceWarehouseOperation.class);

    WarehouseResourceImpl resource = new WarehouseResourceImpl();
    setField(resource, "warehouseRepository", repo);
    setField(resource, "createWarehouseOperation", create);
    setField(resource, "archiveWarehouseOperation", archive);
    setField(resource, "replaceWarehouseOperation", replace);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-1";
    w.location = "AMSTERDAM-001";
    w.capacity = 10;
    w.stock = 1;
    when(repo.getAll()).thenReturn(List.of(w));
    when(repo.findByBusinessUnitCode("BU-1")).thenReturn(w);

    var list = resource.listAllWarehousesUnits();
    assertEquals(1, list.size());
    assertEquals("BU-1", list.get(0).getBusinessUnitCode());

    var single = resource.getAWarehouseUnitByID("BU-1");
    assertEquals("BU-1", single.getBusinessUnitCode());
  }

  @Test
  public void getByIdNotFoundReturns404() {
    WarehouseRepository repo = mock(WarehouseRepository.class);

    WarehouseResourceImpl resource = new WarehouseResourceImpl();
    setField(resource, "warehouseRepository", repo);

    when(repo.findByBusinessUnitCode("MISSING")).thenReturn(null);
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.getAWarehouseUnitByID("MISSING"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void createMapsDomainAndHandlesValidationError() {
    WarehouseRepository repo = mock(WarehouseRepository.class);
    CreateWarehouseOperation create = mock(CreateWarehouseOperation.class);

    WarehouseResourceImpl resource = new WarehouseResourceImpl();
    setField(resource, "warehouseRepository", repo);
    setField(resource, "createWarehouseOperation", create);

    var api = new com.warehouse.api.beans.Warehouse();
    api.setBusinessUnitCode("BU-2");
    api.setLocation("NOPE-001");
    api.setCapacity(10);
    api.setStock(1);

    doThrow(new IllegalArgumentException("bad")).when(create).create(any(Warehouse.class));

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.createANewWarehouseUnit(api));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void archiveHandlesNotFoundAndAlreadyArchived() {
    WarehouseRepository repo = mock(WarehouseRepository.class);
    ArchiveWarehouseOperation archive = mock(ArchiveWarehouseOperation.class);

    WarehouseResourceImpl resource = new WarehouseResourceImpl();
    setField(resource, "warehouseRepository", repo);
    setField(resource, "archiveWarehouseOperation", archive);

    when(repo.findByBusinessUnitCode("MISSING")).thenReturn(null);
    WebApplicationException notFound =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("MISSING"));
    assertEquals(404, notFound.getResponse().getStatus());

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-3";
    when(repo.findByBusinessUnitCode("BU-3")).thenReturn(w);
    doThrow(new IllegalArgumentException("already archived")).when(archive).archive(any(Warehouse.class));
    WebApplicationException badRequest =
        assertThrows(WebApplicationException.class, () -> resource.archiveAWarehouseUnitByID("BU-3"));
    assertEquals(400, badRequest.getResponse().getStatus());
  }

  @Test
  public void replaceHandlesValidationAndReturnsUpdated() {
    WarehouseRepository repo = mock(WarehouseRepository.class);
    ReplaceWarehouseOperation replace = mock(ReplaceWarehouseOperation.class);

    WarehouseResourceImpl resource = new WarehouseResourceImpl();
    setField(resource, "warehouseRepository", repo);
    setField(resource, "replaceWarehouseOperation", replace);

    var api = new com.warehouse.api.beans.Warehouse();
    api.setLocation("AMSTERDAM-001");
    api.setCapacity(10);
    api.setStock(1);

    doThrow(new IllegalArgumentException("nope")).when(replace).replace(any(Warehouse.class));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.replaceTheCurrentActiveWarehouse("BU-4", api));
    assertEquals(400, ex.getResponse().getStatus());

    // success path returns mapped warehouse
    doNothing().when(replace).replace(any(Warehouse.class));
    Warehouse updated = new Warehouse();
    updated.businessUnitCode = "BU-4";
    updated.location = "AMSTERDAM-001";
    updated.capacity = 10;
    updated.stock = 1;
    when(repo.findByBusinessUnitCode("BU-4")).thenReturn(updated);

    var response = resource.replaceTheCurrentActiveWarehouse("BU-4", api);
    assertEquals("BU-4", response.getBusinessUnitCode());
  }
}

