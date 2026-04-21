# Design Patterns Used in the Project

This document outlines the core Design Patterns implemented throughout the Server and Client architecture. All developers (including AI agents) must strictly adhere to these patterns when generating new code to maintain architectural consistency.

## 1. Repository Pattern
- **Location:** Database Access Layer (`server/repository/` and `server/repository/impl/`).
- **Purpose:** To completely decouple database query logic (JPA/Hibernate, JPQL) from the business logic layer (Service).
- **Rule:** The Service layer is strictly prohibited from interacting with `EntityManager` or `Query` objects. All database search and persistence operations must be delegated to Repository interfaces.

## 2. Template Method / Execute Around Pattern
- **Location:** The `AbstractGenericRepositoryImpl` class.
- **Purpose:** To eliminate boilerplate code when managing the Transaction lifecycle (try-catch, begin, commit, rollback, finally close).
- **Rule:** All Repository implementations MUST `extends AbstractGenericRepositoryImpl`. Any database operation must be wrapped inside `doInTransaction(em -> { ... })` for INSERT/UPDATE/DELETE, or `doWithEntityManager(em -> { ... })` for SELECT operations.

## 3. Data Transfer Object (DTO) Pattern
- **Location:** All network communication between Client and Server (via Socket), and communication between Router and Service.
- **Purpose:** To prevent leakage of complex JPA Entities (which contain `@ManyToOne`, Lazy Loading) across the network, avoiding `StackOverflow` errors and hiding the DB structure. It optimizes payload size by only transferring what the UI needs.
- **Rule:** Entities are NEVER allowed to cross the network boundary. The Service layer MUST receive `DTO`s from the network, use Mappers to convert them to `Entity` objects for processing, and return a `Response<DTO>`.

## 4. Command / Router Pattern
- **Location:** Network Layer (`NetworkHandler`, `RequestRouter`, `ActionType`).
- **Purpose:** To decouple the Socket request handling flow. The client sends a command (e.g., `CREATE_SCHEDULE`), and the Router identifies the `ActionType` to dispatch the Request to the appropriate Service via a `switch-case`.
- **Rule:** The Router layer is exclusively for routing. It is strictly forbidden to implement Business Logic (like date validation or price calculation) inside the Router.

## 5. Builder Pattern
- **Location:** DTO and Entity classes.
- **Purpose:** To make the initialization of objects with many properties explicit, readable, and safe, rather than relying on long constructors or scattered setter calls.
- **Rule:** Use Lombok's `@Builder` annotation by default for all DTOs and Entities. Avoid piecemeal setter initialization if a Builder can be used upfront.

## 6. Singleton Pattern
- **Location:** `JPAUtils` and Mapper classes.
- **Purpose:** To conserve memory resources.
- **Rule:** `EntityManagerFactory` is extremely heavy and MUST be instantiated only once per application lifecycle (via a static block in `JPAUtils`). Similarly, `JacksonDataMapper` should be initialized as a `static final` singleton for each Mapper class.

## 7. Facade Pattern
- **Location:** `Service` layer (e.g., `ScheduleServiceImpl`).
- **Purpose:** To group multiple complex operations across different Repositories into a single, cohesive method that is easy for external layers to call.
- **Rule:** For example, when creating a schedule, the Router simply calls `createSchedule()`. The Service acts as a Facade: it orchestrates the `TrainRepository` and `ScheduleRepository`, performs validation, handles exceptions, and returns a unified `Response.success()`.
