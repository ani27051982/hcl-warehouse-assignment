## Unit-testing documentation

This project uses **JUnit 5** (and **Mockito** where needed) plus **Quarkus test utilities** to validate behavior across:

- **Pure unit tests** (fast, no Quarkus boot)
- **Component/unit tests** (unit tests around mappers/observers)
- **Quarkus integration tests** (REST + persistence wiring)
- **Environment-dependent integration tests** (PostgreSQL / Testcontainers)

The goal is to cover **positive**, **negative**, and **error/exception** conditions for each layer.

---

### How to run tests

From `hackathon-java-assignment/`:

- Run all tests:

```bash
./mvnw test
```

- Enforce coverage gate (fails build if coverage < 80%):

```bash
./mvnw verify
```

- Run a single test class:

```bash
./mvnw test -Dtest=StoreResourceTest
```

---

### Test design: positive, negative, error conditions

#### Positive tests (happy paths)

These tests verify correct behavior and correct output for valid inputs.

- **Products REST happy paths**: `ProductResourceTest`
  - Create + list products (201/200)
  - Update + delete flow (200/204)
- **Stores REST happy paths**: `StoreResourceTest`
  - Create + list stores (201/200)
  - Patch + update + delete flow (200/204)
- **Warehouses domain happy paths**: `WarehouseUseCasesUnitTest`
  - Create sets `createdAt`, keeps `archivedAt` null
  - Replace updates mutable fields
  - Archive sets `archivedAt`

#### Negative tests (validation and rule failures)

These tests assert that invalid inputs are rejected with the correct error type/status.

- **Products REST validation**: `ProductResourceTest`
  - Create-with-id → 422
  - Update without name → 422
  - Update/delete non-existing → 404
- **Stores REST validation**: `StoreResourceTest`, `StoreResourceUnitValidationTest`
  - Create-with-id → 422
  - Patch missing name → 422
  - Update non-existing → 404
  - Direct unit validation checks for 422 without needing Quarkus boot
- **Warehouses domain validation**: `WarehouseUseCasesUnitTest`, `WarehouseValidationTest`
  - Duplicate business unit code
  - Invalid location identifier
  - Capacity constraints (exceeds location max)
  - Stock constraints (stock > capacity)
  - Replace not-found, replace archived
  - Archive not-found, archive already archived

#### Error/exception tests (mapping and infrastructure errors)

These tests validate how exceptions are converted to responses or handled.

- **Error mapper unit tests**
  - `StoreComponentsUnitTest`: `StoreResource.ErrorMapper` mapping
  - `ProductComponentsUnitTest`: `ProductResource.ErrorMapper` mapping
- **Transactional / event timing correctness**
  - `StoreTransactionIntegrationTest`, `StoreEventObserverTest`

---

### Warehouse integration & concurrency tests

These tests validate correctness under concurrency and realistic persistence wiring.

- **Concurrency**: `WarehouseConcurrencyIT`
  - Concurrent reads are non-blocking
  - Concurrent creates behave correctly (unique vs duplicate codes)
- **Testcontainers**: `WarehouseTestcontainersIT`
  - Validates behavior against a real PostgreSQL started via Testcontainers (when enabled)
- **External PostgreSQL integration test**: `WarehouseEndpointIT`
  - `@QuarkusIntegrationTest` smoke test against packaged app + PostgreSQL (CI provides DB)

---

### What JUnit test cases exist (by category)

- **Positive paths**
  - `ProductResourceTest`, `StoreResourceTest`, `WarehouseUseCasesUnitTest`, `WarehouseApiResourceTest`
- **Negative/validation paths**
  - `ProductResourceTest`, `StoreResourceTest`, `StoreResourceUnitValidationTest`,
    `WarehouseUseCasesUnitTest`, `WarehouseValidationTest`, `WarehouseSearchResourceTest`
- **Error/exception paths**
  - `ProductComponentsUnitTest`, `StoreComponentsUnitTest`, `WarehouseOptimisticLockingTest`
- **Repository mapping/query behavior**
  - `WarehouseRepositoryUnitTest`, `DbWarehouseUnitTest`

