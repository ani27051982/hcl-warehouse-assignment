package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class DbWarehouseUnitTest {

  @Test
  public void toWarehouseMapsAllFields() {
    DbWarehouse db = new DbWarehouse();
    db.businessUnitCode = "BU-1";
    db.location = "AMSTERDAM-001";
    db.capacity = 10;
    db.stock = 2;
    db.createdAt = LocalDateTime.now().minusDays(1);
    db.archivedAt = LocalDateTime.now();

    var w = db.toWarehouse();
    assertEquals("BU-1", w.businessUnitCode);
    assertEquals("AMSTERDAM-001", w.location);
    assertEquals(10, w.capacity);
    assertEquals(2, w.stock);
    assertEquals(db.createdAt, w.createdAt);
    assertEquals(db.archivedAt, w.archivedAt);
  }
}

