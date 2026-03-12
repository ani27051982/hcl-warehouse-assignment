package com.fulfilment.application.monolith.products;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.quarkus.panache.common.Sort;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ProductResourceUnitTest {

  @Test
  public void getListsProductsSortedByName() {
    ProductRepository repo = mock(ProductRepository.class);
    ProductResource resource = new ProductResource();
    resource.productRepository = repo;

    when(repo.listAll(any(Sort.class))).thenReturn(List.of(new Product("A"), new Product("B")));

    List<Product> products = resource.get();
    assertEquals(2, products.size());
    verify(repo, times(1)).listAll(any(Sort.class));
  }

  @Test
  public void getSingleReturns404WhenMissing() {
    ProductRepository repo = mock(ProductRepository.class);
    ProductResource resource = new ProductResource();
    resource.productRepository = repo;

    when(repo.findById(1L)).thenReturn(null);
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.getSingle(1L));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void createValidatesIdAndPersists() {
    ProductRepository repo = mock(ProductRepository.class);
    ProductResource resource = new ProductResource();
    resource.productRepository = repo;

    Product invalid = new Product("X");
    invalid.id = 99L;
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.create(invalid));
    assertEquals(422, ex.getResponse().getStatus());

    Product valid = new Product("Y");
    valid.id = null;
    var response = resource.create(valid);
    assertEquals(201, response.getStatus());
    verify(repo, times(1)).persist(valid);
  }

  @Test
  public void updateValidationsAndHappyPath() {
    ProductRepository repo = mock(ProductRepository.class);
    ProductResource resource = new ProductResource();
    resource.productRepository = repo;

    Product missingName = new Product();
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> resource.update(1L, missingName));
    assertEquals(422, ex.getResponse().getStatus());

    Product update = new Product("NEW");
    update.description = "d";
    update.price = new BigDecimal("1.00");
    update.stock = 2;

    when(repo.findById(1L)).thenReturn(null);
    WebApplicationException notFound = assertThrows(WebApplicationException.class, () -> resource.update(1L, update));
    assertEquals(404, notFound.getResponse().getStatus());

    Product entity = new Product("OLD");
    entity.id = 1L;
    when(repo.findById(1L)).thenReturn(entity);

    Product result = resource.update(1L, update);
    assertEquals("NEW", result.name);
    assertEquals(2, result.stock);
    verify(repo, atLeastOnce()).persist(entity);
  }

  @Test
  public void deleteValidationsAndHappyPath() {
    ProductRepository repo = mock(ProductRepository.class);
    ProductResource resource = new ProductResource();
    resource.productRepository = repo;

    when(repo.findById(1L)).thenReturn(null);
    WebApplicationException notFound = assertThrows(WebApplicationException.class, () -> resource.delete(1L));
    assertEquals(404, notFound.getResponse().getStatus());

    Product entity = new Product("DEL");
    entity.id = 2L;
    when(repo.findById(2L)).thenReturn(entity);

    var response = resource.delete(2L);
    assertEquals(204, response.getStatus());
    verify(repo, times(1)).delete(entity);
  }
}

