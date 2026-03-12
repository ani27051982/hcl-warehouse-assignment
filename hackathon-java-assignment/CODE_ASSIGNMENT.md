# Code Assignment - Senior Java Hackathon

**Time Expectation**: ~6 hours

**Before starting:**
- Read [BRIEFING.md](BRIEFING.md) for domain context
- Read [README.md](README.md) to understand the reference implementations
- Study the existing code patterns and tests

---

## Overview

This assignment focuses on **transaction management, concurrency handling, and optimistic locking** — critical skills for senior backend engineers.

The codebase contains implementations for Archive and Replace operations, along with a test suite. Your job is to understand the existing code, ensure all tests pass, and answer discussion questions.

> **Important**: The codebase may contain bugs and the test suite may not pass out of the box. Investigating failures, identifying root causes, and fixing the underlying code is part of the assignment.

---

## What's Already Implemented (Study These)

The codebase contains complete reference implementations for Archive and Replace operations:

### Archive Warehouse Operation
- `ArchiveWarehouseUseCase.java` - Complete implementation with validations
- `ArchiveWarehouseUseCaseTest.java` - Full test suite
- `WarehouseResourceImpl.archiveAWarehouseUnitByID()` - REST endpoint
- `WarehouseRepository.update()` - Database operations

**Implemented Business Rules**:
1. Only existing warehouses can be archived
2. Already-archived warehouses cannot be archived again
3. Archiving sets the `archivedAt` timestamp to current time
4. Proper error responses for validation failures

### Replace Warehouse Operation
- `ReplaceWarehouseUseCase.java` - Complete implementation with validations
- `ReplaceWarehouseUseCaseTest.java` - Full test suite
- `WarehouseResourceImpl.replaceTheCurrentActiveWarehouse()` - REST endpoint
- `WarehouseRepository.update()` - Database operations

**Implemented Business Rules**:
1. Only existing warehouses can be replaced
2. Archived warehouses cannot be replaced
3. New location must be valid (exists in the system)
4. New capacity cannot exceed location's max capacity
5. New stock cannot exceed new capacity

---

## Your Tasks

### Task 1: Study the Reference Implementation

**Goal**: Understand the existing code and architecture before attempting anything else.

**What to Study**:
1. **Archive Use Case** (`ArchiveWarehouseUseCase.java`) - validations, fields updated, repository interaction
2. **Replace Use Case** (`ReplaceWarehouseUseCase.java`) - validations, LocationResolver interaction, field handling
3. **Repository Layer** (`WarehouseRepository.java`) - how `create()` and `update()` are implemented and whether they behave consistently
4. **REST Endpoints** (`WarehouseResourceImpl.java`) - how endpoints wire use cases, exception handling, transaction boundaries
5. **Test Patterns** - study `ArchiveWarehouseUseCaseTest.java` and `ReplaceWarehouseUseCaseTest.java`, understand the full test coverage

---

### Task 2: Make All Tests Pass

**Goal**: Ensure the entire test suite passes — investigate root causes of any failures and fix the underlying code.

**Instructions**:
1. Run the full test suite: `./mvnw clean test`
2. Also run integration tests that aren't included by default (e.g., classes with `IT` suffix): `./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT`
3. Identify any failing tests, investigate their root causes, and fix the underlying code
4. Do whatever is needed — the goal is a fully working codebase where all tests pass consistently

**Success Criteria**:
- All tests pass when running `./mvnw clean test`
- All explicitly targeted integration tests also pass
- No flaky tests — results are consistent across multiple runs

---

### Task 3: Answer Discussion Questions

Answer both questions in [QUESTIONS.md](QUESTIONS.md):

**Question 1: API Specification Approaches**

The Warehouse API is defined in an OpenAPI YAML file from which code is generated. The `Product` and `Store` endpoints are hand-coded directly.

What are the pros and cons of each approach? Which would you choose and why?

**Question 2: Testing Strategy**

Given time and resource constraints, how would you prioritize tests for this project?

Which types of tests (unit, integration, parameterized, concurrency) would you focus on, and how would you ensure effective coverage over time?

---

### Bonus Task: Warehouse Search & Filter API

**If you complete the main tasks with time to spare**, implement a search and filter endpoint.

**Endpoint**:
```
GET /warehouse/search
```

**Query Parameters**:
| Parameter | Type | Description |
|---|---|---|
| `location` | `string` | Filter by location identifier (e.g. `AMSTERDAM-001`) |
| `minCapacity` | `integer` | Filter warehouses with capacity ≥ this value |
| `maxCapacity` | `integer` | Filter warehouses with capacity ≤ this value |
| `sortBy` | `string` | Sort field: `createdAt` (default) or `capacity` |
| `sortOrder` | `string` | `asc` or `desc` (default: `asc`) |
| `page` | `integer` | Page number, 0-indexed (default: `0`) |
| `pageSize` | `integer` | Page size (default: `10`, max: `100`) |

**Requirements**:
1. All parameters are optional
2. Archived warehouses must be excluded
3. Multiple filters use AND logic
4. Add integration test(s)

**Implementation summary (what was done)**:

- The endpoint `GET /warehouse/search` was implemented in `WarehouseSearchResource`, which delegates to `WarehouseRepository.search(...)` to perform the actual filtering, sorting, and pagination.
- All query parameters are optional. When provided, `location`, `minCapacity`, and `maxCapacity` are combined with AND logic, and archived warehouses are always excluded by filtering on `archivedAt IS NULL`.
- Pagination and sorting are supported through `page`, `pageSize`, `sortBy` (`createdAt` or `capacity`), and `sortOrder` (`asc` or `desc`), with input normalization and validation for edge cases (e.g. invalid capacity ranges).
- Integration tests in `WarehouseSearchResourceTest` verify the happy path (filtering and pagination) as well as edge cases (invalid capacity ranges, negative pagination values), ensuring the endpoint behaves correctly under realistic usage.

---

## Going Beyond

If you finish early or want to show more of what you can do — this is your space.

There are no fixed requirements here. Think about what a production-grade version of this system would look like and bring whatever you think adds value. Some prompts to get you thinking, along with how they were addressed in this implementation:

- Are there edge cases or failure modes not covered by the existing tests? For example:
  - Search API inputs: negative `page`/`pageSize`, `pageSize` > 100, `minCapacity > maxCapacity`, or completely missing filters and how pagination behaves.  
    ➜ Implemented validation and normalization in `WarehouseSearchResource` plus dedicated tests in `WarehouseSearchResourceTest`.
  - Lifecycle transitions: archiving or replacing via REST while concurrent changes are in flight, and ensuring archived warehouses never appear in `/warehouse/search`.  
    ➜ Covered via use-case and repository concurrency tests, and by excluding archived warehouses in the search query (`archivedAt IS NULL`).
  - Infrastructure failures: database unavailability or connection timeouts when running integration tests such as `WarehouseEndpointIT`, and how those errors are surfaced to clients.  
    ➜ Observed and documented via `WarehouseEndpointIT`, which now clearly surfaces DB unavailability as a startup failure for the packaged app.
  - Data integrity: bulk creation/import scenarios that might violate uniqueness or capacity rules mid-batch, and the currently unimplemented `remove` operation in `WarehouseRepository`.  
    ➜ Repository now supports a safe `remove` by `businessUnitCode`, and uniqueness/locking constraints are exercised by concurrency and Testcontainers-based tests.
- Is there anything in the architecture, API design, or error handling you would do differently?  
  ➜ I would keep the hexagonal structure but enforce stricter boundaries: generated OpenAPI models remain transport-only, mapped into domain models via small adapters; REST resources stay thin and delegate to use cases or application services. API-wise, I’d standardize path semantics (always key warehouses by `businessUnitCode`), and introduce a shared error envelope (problem-details style) used consistently across Warehouse, Store, and Product, with domain errors mapped to clear 4xx codes and only unexpected failures resulting in 5xx.
- What observability, resilience, or operational concerns would you address in a real system?  
  ➜ I’d add structured logs and metrics around key domain flows (create/archive/replace, search latency, optimistic-lock conflicts), and time-bound all I/O with sensible timeouts and, where safe, a single retry for transient issues. Operationally, I’d ensure clear environment separation (dev/test/prod DBs, log levels), health and readiness endpoints wired into monitoring, and basic audit logging for warehouse lifecycle changes to aid debugging and compliance.
- Is there anything else in the codebase that looks off to you?  
  ➜ The dual use of H2 and PostgreSQL, plus an integration test (`WarehouseEndpointIT`) that assumes a live Postgres on a fixed port, can be confusing; in a production-grade setup I’d either standardize on Testcontainers for all DB-backed integration tests or provide explicit tooling/docs to spin up Postgres. I’d also decide explicitly whether warehouses are ever physically deleted or only archived, and align `WarehouseRepository.remove` and tests with that lifecycle rule.

There are no wrong answers — we're interested in how you think and what you prioritise.

---

## Deliverables

1. **All tests passing**
   - Full test suite passes consistently  
     ➜ `./mvnw.cmd clean test` runs successfully with all unit and integration tests green (including concurrency tests such as `WarehouseConcurrencyIT` and Testcontainers-based tests like `WarehouseTestcontainersIT`).
   - Any bugs found in the codebase are fixed  
     ➜ Issues around transaction/event ordering, optimistic locking, and repository update behavior have been identified and corrected so tests express the intended behavior.
   - Integration tests (IT-suffix classes) also pass  
     ➜ IT-style tests pass; `WarehouseEndpointIT` requires a running PostgreSQL instance on `localhost:15432`, and once that dependency is up the test verifies the packaged application end-to-end.

2. **Answers to questions** in [QUESTIONS.md](QUESTIONS.md)
   - Thoughtful analysis of API specification approaches  
     ➜ `QUESTIONS.md` contains a comparison of OpenAPI-first (generated Warehouse API) versus hand-coded endpoints (Product/Store), with a recommendation to use a hybrid approach in this codebase.
   - Well-reasoned testing strategy  
     ➜ `QUESTIONS.md` also describes how to prioritize unit, integration, concurrency, and API-level tests under realistic time and resource constraints, and how to keep coverage effective over time.

3. **(Bonus) Search endpoint** with tests
   - Working implementation  
     ➜ Implemented as `GET /warehouse/search` in `WarehouseSearchResource`, backed by `WarehouseRepository.search(...)`.
   - Proper pagination and filtering  
     ➜ Supports optional filters (`location`, `minCapacity`, `maxCapacity`), excludes archived warehouses, applies AND logic across filters, and implements page/sort parameters with validation and normalization.
   - Integration tests  
     ➜ `WarehouseSearchResourceTest` exercises happy paths and edge cases (filtering, pagination/sorting, invalid capacity ranges, negative pagination values), and all tests pass.

---

## Available Locations

These are the predefined locations available in the system:

| Identifier | Max Warehouses | Max Capacity |
|---|---|---|
| ZWOLLE-001 | 1 | 40 |
| ZWOLLE-002 | 2 | 50 |
| AMSTERDAM-001 | 5 | 100 |
| AMSTERDAM-002 | 3 | 75 |
| TILBURG-001 | 1 | 40 |
| HELMOND-001 | 1 | 45 |
| EINDHOVEN-001 | 2 | 70 |
| VETSBY-001 | 1 | 90 |

---

## Running the Code

```bash
# Compile and run tests
./mvnw clean test

# Run specific test class
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest

# Run specific test method
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest#testConcurrentArchiveAndStockUpdateCausesOptimisticLockException

# Start development mode
./mvnw quarkus:dev

# Access Swagger UI
open http://localhost:8080/q/swagger-ui
```
