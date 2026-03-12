package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.List;

@RequestScoped
@Path("/warehouse/search")
@Produces("application/json")
public class WarehouseSearchResource {

  @Inject WarehouseRepository warehouseRepository;

  @GET
  public List<Warehouse> searchWarehouses(
      @QueryParam("location") String location,
      @QueryParam("minCapacity") Integer minCapacity,
      @QueryParam("maxCapacity") Integer maxCapacity,
      @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
      @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
      @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("pageSize") @DefaultValue("10") int pageSize) {

    // Normalize and validate pagination parameters
    if (page < 0) {
      page = 0;
    }
    if (pageSize <= 0) {
      pageSize = 10;
    }

    // Validate capacity range
    if (minCapacity != null && maxCapacity != null && minCapacity > maxCapacity) {
      throw new jakarta.ws.rs.WebApplicationException(
          "minCapacity cannot be greater than maxCapacity", 400);
    }

    var results =
        warehouseRepository.search(
            location, minCapacity, maxCapacity, sortBy, sortOrder, page, pageSize);

    return results.stream()
        .map(
            w -> {
              var response = new Warehouse();
              response.setBusinessUnitCode(w.businessUnitCode);
              response.setLocation(w.location);
              response.setCapacity(w.capacity);
              response.setStock(w.stock);
              return response;
            })
        .toList();
  }
}

