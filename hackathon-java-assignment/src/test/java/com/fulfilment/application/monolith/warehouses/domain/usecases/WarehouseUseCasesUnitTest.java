package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class WarehouseUseCasesUnitTest {

  static class InMemoryWarehouseStore implements WarehouseStore {
    private final Map<String, Warehouse> byCode = new HashMap<>();

    @Override
    public List<Warehouse> getAll() {
      return new ArrayList<>(byCode.values());
    }

    @Override
    public void create(Warehouse warehouse) {
      byCode.put(warehouse.businessUnitCode, copy(warehouse));
    }

    @Override
    public void update(Warehouse warehouse) {
      byCode.put(warehouse.businessUnitCode, copy(warehouse));
    }

    @Override
    public void remove(Warehouse warehouse) {
      byCode.remove(warehouse.businessUnitCode);
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      Warehouse w = byCode.get(buCode);
      return w == null ? null : copy(w);
    }

    private static Warehouse copy(Warehouse w) {
      Warehouse c = new Warehouse();
      c.businessUnitCode = w.businessUnitCode;
      c.location = w.location;
      c.capacity = w.capacity;
      c.stock = w.stock;
      c.createdAt = w.createdAt;
      c.archivedAt = w.archivedAt;
      return c;
    }
  }

  static class FixedLocationResolver implements LocationResolver {
    private final Map<String, Location> locations = new HashMap<>();

    FixedLocationResolver() {
      locations.put("AMSTERDAM-001", new Location("AMSTERDAM-001", 5, 100));
      locations.put("ZWOLLE-001", new Location("ZWOLLE-001", 1, 40));
    }

    @Override
    public Location resolveByIdentifier(String identifier) {
      return locations.get(identifier);
    }
  }

  @Test
  public void createWarehouseHappyPathSetsCreatedAt() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    CreateWarehouseUseCase uc = new CreateWarehouseUseCase(store, resolver);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-1";
    w.location = "AMSTERDAM-001";
    w.capacity = 50;
    w.stock = 10;

    uc.create(w);

    Warehouse stored = store.findByBusinessUnitCode("BU-1");
    assertNotNull(stored);
    assertNotNull(stored.createdAt);
    assertNull(stored.archivedAt);
    assertEquals("AMSTERDAM-001", stored.location);
  }

  @Test
  public void createWarehouseFailsOnDuplicateCode() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    CreateWarehouseUseCase uc = new CreateWarehouseUseCase(store, resolver);

    Warehouse w1 = new Warehouse();
    w1.businessUnitCode = "DUP";
    w1.location = "AMSTERDAM-001";
    w1.capacity = 50;
    w1.stock = 0;
    uc.create(w1);

    Warehouse w2 = new Warehouse();
    w2.businessUnitCode = "DUP";
    w2.location = "AMSTERDAM-001";
    w2.capacity = 50;
    w2.stock = 0;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uc.create(w2));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  public void createWarehouseFailsOnInvalidLocation() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    CreateWarehouseUseCase uc = new CreateWarehouseUseCase(store, resolver);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-2";
    w.location = "NOPE-001";
    w.capacity = 10;
    w.stock = 0;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uc.create(w));
    assertTrue(ex.getMessage().contains("not valid"));
  }

  @Test
  public void createWarehouseFailsOnCapacityExceedingLocationMax() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    CreateWarehouseUseCase uc = new CreateWarehouseUseCase(store, resolver);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-3";
    w.location = "ZWOLLE-001";
    w.capacity = 41; // max is 40
    w.stock = 0;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uc.create(w));
    assertTrue(ex.getMessage().contains("exceeds"));
  }

  @Test
  public void createWarehouseFailsWhenStockExceedsCapacity() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    CreateWarehouseUseCase uc = new CreateWarehouseUseCase(store, resolver);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-4";
    w.location = "AMSTERDAM-001";
    w.capacity = 10;
    w.stock = 11;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uc.create(w));
    assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
  }

  @Test
  public void replaceWarehouseHappyPathUpdatesFields() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    ReplaceWarehouseUseCase uc = new ReplaceWarehouseUseCase(store, resolver);

    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "BU-5";
    existing.location = "AMSTERDAM-001";
    existing.capacity = 50;
    existing.stock = 10;
    existing.createdAt = LocalDateTime.now().minusDays(1);
    store.create(existing);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "BU-5";
    replacement.location = "ZWOLLE-001";
    replacement.capacity = 40;
    replacement.stock = 5;

    uc.replace(replacement);

    Warehouse updated = store.findByBusinessUnitCode("BU-5");
    assertEquals("ZWOLLE-001", updated.location);
    assertEquals(40, updated.capacity);
    assertEquals(5, updated.stock);
    assertNotNull(updated.createdAt);
    assertNull(updated.archivedAt);
  }

  @Test
  public void replaceWarehouseFailsWhenArchived() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    ReplaceWarehouseUseCase uc = new ReplaceWarehouseUseCase(store, resolver);

    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "ARCH";
    existing.location = "AMSTERDAM-001";
    existing.capacity = 50;
    existing.stock = 10;
    existing.createdAt = LocalDateTime.now();
    existing.archivedAt = LocalDateTime.now();
    store.create(existing);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "ARCH";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 50;
    replacement.stock = 10;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uc.replace(replacement));
    assertTrue(ex.getMessage().contains("archived"));
  }

  @Test
  public void replaceWarehouseFailsWhenNotFound() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    ReplaceWarehouseUseCase uc = new ReplaceWarehouseUseCase(store, resolver);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MISSING";
    replacement.location = "AMSTERDAM-001";
    replacement.capacity = 10;
    replacement.stock = 1;

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uc.replace(replacement));
    assertTrue(ex.getMessage().contains("does not exist"));
  }

  @Test
  public void replaceWarehouseFailsWhenLocationInvalidOrRulesBroken() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    FixedLocationResolver resolver = new FixedLocationResolver();
    ReplaceWarehouseUseCase uc = new ReplaceWarehouseUseCase(store, resolver);

    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "BU-8";
    existing.location = "AMSTERDAM-001";
    existing.capacity = 50;
    existing.stock = 10;
    existing.createdAt = LocalDateTime.now();
    store.create(existing);

    Warehouse invalidLoc = new Warehouse();
    invalidLoc.businessUnitCode = "BU-8";
    invalidLoc.location = "NOPE-001";
    invalidLoc.capacity = 10;
    invalidLoc.stock = 1;
    assertThrows(IllegalArgumentException.class, () -> uc.replace(invalidLoc));

    Warehouse capTooHigh = new Warehouse();
    capTooHigh.businessUnitCode = "BU-8";
    capTooHigh.location = "ZWOLLE-001";
    capTooHigh.capacity = 41;
    capTooHigh.stock = 1;
    assertThrows(IllegalArgumentException.class, () -> uc.replace(capTooHigh));

    Warehouse stockTooHigh = new Warehouse();
    stockTooHigh.businessUnitCode = "BU-8";
    stockTooHigh.location = "ZWOLLE-001";
    stockTooHigh.capacity = 10;
    stockTooHigh.stock = 11;
    assertThrows(IllegalArgumentException.class, () -> uc.replace(stockTooHigh));
  }

  @Test
  public void archiveWarehouseHappyPathSetsArchivedAt() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    ArchiveWarehouseUseCase uc = new ArchiveWarehouseUseCase(store);

    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "BU-6";
    existing.location = "AMSTERDAM-001";
    existing.capacity = 10;
    existing.stock = 1;
    existing.createdAt = LocalDateTime.now();
    store.create(existing);

    Warehouse input = new Warehouse();
    input.businessUnitCode = "BU-6";

    uc.archive(input);
    Warehouse archived = store.findByBusinessUnitCode("BU-6");
    assertNotNull(archived.archivedAt);
  }

  @Test
  public void archiveWarehouseFailsWhenAlreadyArchived() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    ArchiveWarehouseUseCase uc = new ArchiveWarehouseUseCase(store);

    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "BU-7";
    existing.archivedAt = LocalDateTime.now();
    store.create(existing);

    Warehouse input = new Warehouse();
    input.businessUnitCode = "BU-7";

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uc.archive(input));
    assertTrue(ex.getMessage().contains("already archived"));
  }

  @Test
  public void archiveWarehouseFailsWhenNotFound() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    ArchiveWarehouseUseCase uc = new ArchiveWarehouseUseCase(store);

    Warehouse input = new Warehouse();
    input.businessUnitCode = "NOPE";

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> uc.archive(input));
    assertTrue(ex.getMessage().contains("does not exist"));
  }
}

