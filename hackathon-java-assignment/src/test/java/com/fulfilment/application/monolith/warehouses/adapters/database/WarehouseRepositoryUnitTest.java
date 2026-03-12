package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class WarehouseRepositoryUnitTest {

  @Test
  public void searchBuildsQueryAndMapsResult() {
    WarehouseRepository repo = spy(new WarehouseRepository());

    @SuppressWarnings("unchecked")
    PanacheQuery<DbWarehouse> query = (PanacheQuery<DbWarehouse>) mock(PanacheQuery.class);

    when(query.page(any(Page.class))).thenReturn(query);

    DbWarehouse row = new DbWarehouse();
    row.businessUnitCode = "BU-X";
    row.location = "AMSTERDAM-001";
    row.capacity = 50;
    row.stock = 10;
    row.createdAt = LocalDateTime.now();
    row.archivedAt = null;

    when(query.list()).thenReturn(List.of(row));

    ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);

    doReturn(query).when(repo).find(q.capture(), sort.capture(), any(Object[].class));

    List<Warehouse> result =
        repo.search("AMSTERDAM-001", 40, 100, "capacity", "desc", 0, 10);

    assertEquals(1, result.size());
    assertEquals("BU-X", result.get(0).businessUnitCode);
    assertEquals("AMSTERDAM-001", result.get(0).location);

    assertTrue(q.getValue().contains("archivedAt IS NULL"));
    assertTrue(q.getValue().contains("location"));
    assertTrue(q.getValue().contains("capacity"));
    assertNotNull(sort.getValue());
  }

  @Test
  public void getAllMapsDbWarehouses() {
    WarehouseRepository repo = spy(new WarehouseRepository());

    DbWarehouse row = new DbWarehouse();
    row.businessUnitCode = "BU-1";
    row.location = "ZWOLLE-001";
    row.capacity = 40;
    row.stock = 1;

    doReturn(List.of(row)).when(repo).listAll();

    List<Warehouse> all = repo.getAll();
    assertEquals(1, all.size());
    assertEquals("BU-1", all.get(0).businessUnitCode);
  }

  @Test
  public void findByBusinessUnitCodeMapsResult() {
    WarehouseRepository repo = spy(new WarehouseRepository());

    @SuppressWarnings("unchecked")
    PanacheQuery<DbWarehouse> query = (PanacheQuery<DbWarehouse>) mock(PanacheQuery.class);

    DbWarehouse row = new DbWarehouse();
    row.businessUnitCode = "BU-2";
    row.location = "ZWOLLE-001";

    when(query.firstResult()).thenReturn(row);
    doReturn(query).when(repo).find(eq("businessUnitCode"), eq("BU-2"));

    Warehouse found = repo.findByBusinessUnitCode("BU-2");
    assertNotNull(found);
    assertEquals("BU-2", found.businessUnitCode);
  }

  @Test
  public void createUpdateAndRemoveInvokePanacheMethods() {
    WarehouseRepository repo = spy(new WarehouseRepository());

    // create delegates to persist
    doNothing().when(repo).persist(any(DbWarehouse.class));
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse w =
        new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    w.businessUnitCode = "C1";
    w.location = "AMSTERDAM-001";
    w.capacity = 10;
    w.stock = 1;
    repo.create(w);
    verify(repo, times(1)).persist(any(DbWarehouse.class));

    // update loads entity and mutates fields
    DbWarehouse db = new DbWarehouse();
    db.businessUnitCode = "C1";
    db.location = "ZWOLLE-001";
    @SuppressWarnings("unchecked")
    PanacheQuery<DbWarehouse> q = (PanacheQuery<DbWarehouse>) mock(PanacheQuery.class);
    when(q.firstResult()).thenReturn(db);
    doReturn(q).when(repo).find(eq("businessUnitCode"), eq("C1"));

    w.location = "AMSTERDAM-001";
    repo.update(w);
    assertEquals("AMSTERDAM-001", db.location);

    // remove delegates to delete(query)
    doReturn(1L).when(repo).delete(eq("businessUnitCode"), eq("C1"));
    repo.remove(w);
    verify(repo, times(1)).delete("businessUnitCode", "C1");
  }
}

