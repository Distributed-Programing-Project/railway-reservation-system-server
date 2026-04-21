# Architecture

## Monorepo Strategy

Currently **one repo**, will be split into **three repos** when the project is complete:

| Repo | Role |
|---|---|
| `common` | Deployed as a Maven artifact to a package registry |
| `server` | Adds `common` as a `pom.xml` dependency |
| `client` | Adds `common` as a `pom.xml` dependency |

Both `client` and `server` communicate via the classes defined in `common` — they never share code directly with each other.

**Implication:** Everything that both sides need (DTOs, enums, Request/Response, ActionType) must live in `common`, not in `server` or `client`.

---

## Module Structure

Three packages under `vn.edu.iuh.fit`:

```
vn.edu.iuh.fit/
├── client/     # JavaFX UI — not yet implemented
├── common/     # Shared Maven artifact — bridge between client and server
└── server/     # Business logic, JPA, database
```

### `common`

```
common/
├── command/
│   └── ActionType.java      # All client→server action names
├── request/
│   └── Request.java         # { ActionType action; Object data; }
└── response/
    └── Response.java        # { boolean success; String message; Object data; }
```

> DTOs are in `server/dto/` and enums in `server/constant/` temporarily.
> They will move to `common/dto/` and `common/enums/` before the repo split.

**Must NOT contain:** `@Entity` classes, Repository/Service, Hibernate config, JavaFX code.

### `server`

```
server/
├── constant/     # Domain enums (→ common/enums/ before split)
├── dto/          # DTOs (→ common/dto/ before split)
├── model/        # JPA entities
├── repository/   # DB access (JPA/JPQL)
├── service/      # Business logic
├── network/      # Socket server + request routing
├── mapper/       # Entity ↔ DTO conversion
└── util/         # Helpers (JPAUtils, ...)
```

Layer order: `network` → `service` → `repository` → `model`

**Must NOT:** expose DB credentials, skip service layer, contain UI code.

### `client`

Not yet implemented. When added: JavaFX UI, sends `Request`, receives `Response`.

**Must NOT:** connect directly to DB, use Hibernate, contain business logic.

---

## Architecture Flow

```
client (JavaFX)
    │
    │  Socket TCP
    │  Request  { ActionType, Object data }   ──▶
    │  Response { boolean, String, Object }   ◀──
    │
server
    │
    │  JPA / Hibernate 7
    ▼
MariaDB (localhost:3307)
```

---

## Entity Relationships

```
Train (1) ──────< Carriage (Many)
Carriage (1) ───< Seat (Many)
Schedule (1) ───< ScheduleDetail (Many)
Seat (1) ───────< ScheduleDetail (Many)
ScheduleDetail (1) ──── Ticket (1)
Customer (1) ───< Ticket (Many)
Customer (1) ───< Invoice (Many)
Employee (1) ───< Invoice (Many)
Invoice (1) ────< InvoiceDetail (Many)
Ticket (1) ─────── InvoiceDetail (1)
```

**Stub entities (not yet implemented):** `Schedule`, `Employee`

---

## Database Configuration

Persistence unit: `mariadb-pu` (in `src/main/resources/META-INF/persistence.xml`)

| Property | Value |
|---|---|
| Host | localhost:3307 |
| Driver | MariaDB |
| hbm2ddl.auto | update |

`JPAUtils` in `server/util/` exposes a singleton `EntityManagerFactory` and `getEntityManager()`.
