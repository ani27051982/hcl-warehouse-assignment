package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    this.persist(dbWarehouse);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse == null) {
      return;
    }

    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    if (warehouse == null || warehouse.businessUnitCode == null) {
      return;
    }
    delete("businessUnitCode", warehouse.businessUnitCode);
  }

  @Override
  @Transactional
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }

  /**
   * Search warehouses using optional filters and pagination.
   *
   * - Excludes archived warehouses (archivedAt IS NULL)
   * - Applies AND logic across all provided filters
   * - Supports sorting by createdAt (default) or capacity, asc/desc
   */
  public List<Warehouse> search(
      String location,
      Integer minCapacity,
      Integer maxCapacity,
      String sortBy,
      String sortOrder,
      int page,
      int pageSize) {

    // Base: exclude archived warehouses
    StringBuilder query = new StringBuilder("archivedAt IS NULL");
    List<Object> params = new ArrayList<>();

    if (location != null && !location.isBlank()) {
      query.append(" AND location = ?1");
      params.add(location);
    }

    if (minCapacity != null) {
      query.append(params.isEmpty() ? " capacity >= ?" + (params.size() + 1) : " AND capacity >= ?" + (params.size() + 1));
      params.add(minCapacity);
    }

    if (maxCapacity != null) {
      query.append(params.isEmpty() ? " capacity <= ?" + (params.size() + 1) : " AND capacity <= ?" + (params.size() + 1));
      params.add(maxCapacity);
    }

    String sortField = "createdAt";
    if ("capacity".equalsIgnoreCase(sortBy)) {
      sortField = "capacity";
    }

    Sort.Direction direction =
        "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.Descending : Sort.Direction.Ascending;

    var panacheQuery =
        find(query.toString(), Sort.by(sortField, direction), params.toArray())
            .page(Page.of(page, Math.min(pageSize, 100)));

    return panacheQuery.list().stream().map(DbWarehouse::toWarehouse).toList();
  }
}
