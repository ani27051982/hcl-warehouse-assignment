## Code Coverage Journey (24% → 87%)

This document summarizes the changes made to increase JaCoCo test coverage from **~24%** to **87%** for the `java-code-assignment` module.

---

### 1. Enabling Coverage Reporting

- **Added JaCoCo Maven plugin** in `pom.xml` to automatically attach the JaCoCo agent and generate HTML reports under `target/site/jacoco`:
  - `jacoco-maven-plugin` with:
    - `prepare-agent` execution.
    - `report` execution bound to the `test` phase.

**Outcome**: Coverage reports became available to guide where to add/adjust tests.

---

### 2. Fixing Existing Test Failures

Before improving coverage, several existing tests were failing and blocking a clean build.

- **Store event transaction issues**
  - `StoreEventObserver`:
    - Changed `@ObservesAsync` to `@Observes(during = TransactionPhase.AFTER_SUCCESS)` for `StoreCreatedEvent` and `StoreUpdatedEvent`.
  - `StoreResource`:
    - Changed `storeCreatedEvent.fireAsync(...)` to `storeCreatedEvent.fire(...)`.
    - Changed `storeUpdatedEvent.fireAsync(...)` to `storeUpdatedEvent.fire(...)`.
  - **Reason**: Ensure legacy-system synchronization only runs after a successful transaction commit and within the same transactional context.

- **Optimistic locking / lost update issues**
  - `WarehouseRepository.update(...)`:
    - Replaced the bulk JPQL `UPDATE` with an entity fetch-and-modify approach:
      - Load `DbWarehouse` by `businessUnitCode`.
      - Mutate fields (`location`, `capacity`, `stock`, `archivedAt`).
      - Rely on JPA + `@Version` for optimistic locking at commit time.

- **Transactional context in concurrency tests**
  - `WarehouseRepository`:
    - Added `@Transactional` to `create(...)`, `update(...)`, and `findByBusinessUnitCode(...)`.
  - `WarehouseConcurrencyIT`:
    - Added `@Transactional` helper `findWarehouseByBusinessUnitCode(...)` and invoked it from concurrent threads.
  - **Reason**: Ensure proper CDI/JTA context in multi-threaded tests to avoid `ContextNotActiveException` and flaky results.

- **Import data and optimistic locking**
  - `import.sql`:
    - Updated all `warehouse` `INSERT` statements to set `version = 0`.
  - **Reason**: Prevent `NullPointerException` involving the `@Version` field in `DbWarehouse` during updates.

**Outcome**: All existing tests (excluding environment-dependent ones like `WarehouseEndpointIT` when PostgreSQL is not running) could pass reliably, creating a stable baseline for coverage work.

---

### 3. Implementing the Warehouse Search & Filter API (Bonus Task)

Although primarily a feature task, this significantly contributed to coverage.

- **Repository enhancements**
  - `WarehouseRepository`:
    - Implemented `search(...)` to support:
      - Filtering by `location`.
      - Capacity range (`minCapacity`, `maxCapacity`).
      - Pagination (`page`, `pageSize`).
      - Sorting (`sortBy`, `sortOrder`).
      - Exclusion of archived warehouses (`archivedAt IS NULL`).

- **New REST resource**
  - `WarehouseSearchResource`:
    - New endpoint `GET /warehouse/search`.
    - Validation and normalization logic:
      - Non-negative `page`.
      - Positive `pageSize` with sensible default.
      - `minCapacity <= maxCapacity` check (400 on invalid range).

- **Integration tests**
  - `WarehouseSearchResourceTest`:
    - `testSearchFiltersAndExcludesArchived()`.
    - `testSearchPaginationAndSorting()`.
    - `testInvalidCapacityRangeReturnsBadRequest()`.
    - `testNegativePageAndPageSizeAreNormalized()`.

**Outcome**: High coverage of the new search feature (repository + REST layer) and validation paths.

---

### 4. Expanding REST API Tests (Products & Stores)

To raise coverage for the `products` and `stores` packages, several integration-level tests were added.

- **`ProductResourceTest`**
  - New tests:
    - `testUpdateAndDeleteProduct()`:
      - Covers update then delete flows, including success responses.
    - `testUpdateValidationAndNotFoundPaths()`:
      - Exercises validation failures and 404 scenarios for updates.

- **`StoreResourceTest`**
  - New tests:
    - `testUpdateAndDeleteStore()`:
      - Covers update and delete operations, including success responses.
    - `testPatchAndValidationPaths()`:
      - Exercises PATCH behavior and validation/error cases.

**Outcome**: Coverage for `com.fulfilment.application.monolith.products` and `com.fulfilment.application.monolith.stores` increased significantly (products nearly fully covered).

---

### 5. Unit Tests for Error Mappers and Observers

Some logic is better exercised with pure unit tests rather than full Quarkus integration tests.

- **`StoreComponentsUnitTest`** (new)
  - Unit tests for:
    - `StoreEventObserver` (event handling behavior).
    - `LegacyStoreManagerGateway` (delegation to external/legacy integration).
    - `StoreResource.ErrorMapper` (HTTP mapping for exceptions).

- **`ProductComponentsUnitTest`** (new)
  - Unit tests for:
    - `ProductResource.ErrorMapper` (HTTP mapping for exceptions).

**Outcome**: Improved coverage for component-level classes that are otherwise hard to hit through only REST tests.

---

### 6. Domain Use Case Unit Tests

The core business logic for warehouses lives in the `usecases` package. These were given dedicated, fast unit tests.

- **`WarehouseUseCasesUnitTest`** (new)
  - In-memory fakes for `WarehouseStore` to avoid database/proxy overhead.
  - Tests for:
    - `CreateWarehouseUseCase`:
      - Happy path creation.
      - Validation / error paths (e.g., missing fields, duplicate codes).
    - `ReplaceWarehouseUseCase`:
      - Replacing existing warehouses.
      - Handling of not-found and invalid data.
    - `ArchiveWarehouseUseCase`:
      - Archiving lifecycle transitions.
      - Edge cases around repeated archives or invalid states.

**Outcome**: Achieved **100% coverage** for `com.fulfilment.application.monolith.warehouses.domain.usecases`.

---

### 7. Database Adapter Unit Tests

To ensure repository-level behavior was covered without always going through Quarkus integration:

- **`WarehouseRepositoryUnitTest`** (new)
  - Uses Mockito to:
    - Mock Panache operations (`find`, `listAll`, `delete`, etc.).
    - Verify:
      - Query construction and parameter passing in `search(...)`.
      - Correct mapping for `getAll()` and `findByBusinessUnitCode(...)`.
      - Behavior of `create(...)`, `update(...)`, and `remove(...)`.

- **`DbWarehouseUnitTest`** (new)
  - Verifies:
    - `DbWarehouse.toWarehouse()` mapping logic.

**Outcome**: High coverage and confidence for `com.fulfilment.application.monolith.warehouses.adapters.database`.

---

### 8. Warehouse REST API Tests

The main warehouse REST API (`WarehouseResourceImpl` and generated interfaces/beans) received focused test coverage.

- **`WarehouseApiResourceTest`** (new)
  - Covers:
    - Listing all warehouses.
    - Getting by code.
    - Creating warehouses (happy path & validation).
    - Archiving warehouses and validating lifecycle behavior.
    - Replacing warehouses and validating responses.
  - Ensures isolation by creating dedicated warehouses for archival and replacement tests.

- **`WarehouseResourceImplUnitTest`** (new)
  - Uses Mockito and reflection to:
    - Inject mocks for use cases and repository.
    - Test:
      - Happy paths.
      - Not-found scenarios.
      - Validation and error situations for key operations.

**Outcome**: Coverage for `com.fulfilment.application.monolith.warehouses.adapters.restapi` increased to over 90%.

---

### 9. Store Validation Unit Tests (Final Coverage Boost)

To push the overall coverage target beyond 85%, dedicated unit tests were added for store validation paths.

- **`StoreResourceUnitValidationTest`** (new)
  - Directly constructs `StoreResource` and calls its methods to exercise validation logic:
    - `createWithIdReturns422`:
      - Creating a `Store` with a preset `id` must return HTTP 422.
    - `updateWithoutNameReturns422`:
      - Updating a `Store` without a `name` must return HTTP 422.
    - `patchWithoutNameReturns422`:
      - Patching a `Store` without a `name` must return HTTP 422.

**Outcome**: Additional coverage for validation branches in the stores REST layer, helping move overall coverage from ~84% to **87%**.

---

### 10. Final Coverage Status

After all of the above changes:

- **Overall instruction coverage** (JaCoCo `index.html`):
  - **87%** instructions covered.
- Notable package-level coverage:
  - `com.fulfilment.application.monolith.products`: ~97% instructions.
  - `com.fulfilment.application.monolith.warehouses.domain.usecases`: 100%.
  - `com.fulfilment.application.monolith.warehouses.adapters.database`: ~93%.
  - `com.fulfilment.application.monolith.warehouses.adapters.restapi`: ~93%.
  - `com.fulfilment.application.monolith.location`: 100%.

This document can be referenced in reviews to clearly show how coverage was raised from approximately **24%** to **87%**, and where the key investments in tests and fixes were made.

