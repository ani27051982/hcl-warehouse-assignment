# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
Using an OpenAPI-first approach for the Warehouse API has some clear advantages:
- The API contract is explicitly defined and versionable, which makes it easy to align backend, frontend, and other consumers.
- Code generation reduces boilerplate for DTOs, interfaces, and documentation wiring, and keeps the docs in sync with the implementation.
- It enforces consistent naming, HTTP semantics, and error shapes across teams, especially valuable in larger organizations.
- Tooling support (Swagger UI, client generation in multiple languages, schema validation) comes “for free” once the spec is in place.

However, it also has downsides:
- Generated code can be verbose or opinionated, sometimes fighting with the project's preferred patterns (e.g., hexagonal architecture, domain models).
- Refactoring via IDE is less straightforward because some classes are generated, not hand-written.
- There is a risk that the team treats the spec as an afterthought, letting it drift unless there is discipline and automation.
- For very small or rapidly evolving services, maintaining the spec in lockstep can feel like overhead compared to just coding handlers.

Hand-coded endpoints like `Product` and `Store` provide:
- Maximum flexibility in how resources are modeled, how errors are shaped, and how the layers are structured.
- Faster iteration in the early phase of a service when the API is still exploratory.
- Less dependency on code generators and their quirks.

But the trade-offs are:
- The “spec” effectively lives in scattered annotations and code, which can make it harder to share with other teams and consumers.
- Documentation can lag behind behavior unless you invest in manual docs or annotation-driven OpenAPI generation.
- Consistency across teams and services depends more on conventions and code review than on a single source of truth.

Given this project’s domain (warehouses, concurrency, rules) and the fact that the Warehouse API is already spec-driven, I would:
- Prefer OpenAPI-first for externally consumed, business-critical APIs like `Warehouse`, where contract clarity and client generation matter.
- Accept hand-coded endpoints for smaller, internal, or low-risk domains (like simple admin / support endpoints) where speed of change is more important than formal contract management.
In a real system I would aim for a hybrid: core public APIs defined in OpenAPI with codegen, plus hand-written internal endpoints, all wired through the same domain/use-case layer so that generated models never leak into the core domain.
```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
For this project I would prioritize tests based on risk and behavior:

1) Core business rules (unit and domain tests)
- Focus first on unit tests around the use cases (`CreateWarehouseUseCase`, `ArchiveWarehouseUseCase`, `ReplaceWarehouseUseCase`).
- These are cheap to run, easy to reason about, and directly protect key invariants:
  - uniqueness of business unit codes
  - location validity and capacity constraints
  - stock vs capacity rules
  - archiving / replacement preconditions.
- I would keep these tests fast, deterministic, and free of I/O where possible.

2) Integration tests for persistence and transactions
- Use integration tests (like the existing `ArchiveWarehouseUseCaseTest`, `WarehouseConcurrencyIT`, and Testcontainers-based tests) to verify:
  - JPA mappings (`DbWarehouse`, version field) behave as expected.
  - Transactions and optimistic locking detect conflicts instead of silently losing data.
  - Repository methods (`create`, `update`, search/filter if added) work end-to-end against a real database.
- These don’t need to cover every branch, but they should cover the happy paths and the most important failure modes (constraint violations, concurrent updates).

3) Concurrency and race-condition tests
- Keep a small number of targeted concurrency tests (like `WarehouseConcurrencyIT` and the concurrent archive/update tests).
- These are slower and more complex to maintain, so I’d limit them to a few high-value scenarios:
  - concurrent creation with unique and duplicate codes
  - concurrent archive vs stock update
  - concurrent reads to ensure scalability.

4) API-level tests / contract tests
- Add a thin layer of tests at the REST boundary to:
  - validate serialization/deserialization
  - check HTTP status codes and error payloads for key scenarios.
- These can be fewer in number, focusing on representative flows (create, get, archive, replace) rather than exhaustive combinations.

Keeping coverage effective over time:
- Run unit tests on every push / PR; run the full integration + concurrency suite in CI on main or in nightly pipelines.
- Use coverage reports as a guide, but focus on critical paths (use cases, repository behavior, transaction boundaries) rather than chasing 100% coverage.
- When fixing a bug, always add or extend a test that would have caught it, so the suite evolves with the system.
- Keep tests readable and intent-revealing so that future contributors can safely refactor code with confidence supported by the suite.
```
