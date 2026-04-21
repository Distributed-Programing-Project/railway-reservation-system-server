Generate the full vertical stack for a usecase based on its documentation in `docs/usecases/`.
Runs in two phases: **Plan → confirm → Generate**. Never write code before the user approves the plan.

## Input

`$ARGUMENTS` — the usecase name, matching the filename in `docs/usecases/` (e.g. `buy-ticket`, `login`)

---

## Phase 1 — Plan

### Step 1: Read everything in parallel
- `docs/usecases/$ARGUMENTS.md` — the usecase description (required — stop if not found)
- `src/main/java/vn/edu/iuh/fit/server/model/` — existing entities
- `src/main/java/vn/edu/iuh/fit/server/dto/` — existing DTOs
- `src/main/java/vn/edu/iuh/fit/server/constant/` — existing enums
- `src/main/java/vn/edu/iuh/fit/common/command/ActionType.java` — existing actions
- `src/main/java/vn/edu/iuh/fit/server/repository/` — existing repositories (if any)
- `src/main/java/vn/edu/iuh/fit/server/service/` — existing services (if any)

### Step 2: Analyze the usecase
- Which entities are involved and how do they relate?
- What operations are needed (create / read / update / delete)?
- What validations and business rules must be enforced?
- What data flows from client → server → DB and back?

### Step 3: Identify risks
Look for problems that would block implementation or cause bugs later:
- Race conditions (e.g. two clients booking the same seat simultaneously)
- Missing data (usecase needs a field no entity currently stores)
- Circular dependency between services
- Transaction scope issues (operation spans multiple aggregates)
- Serialization risk (object sent over socket that doesn't implement Serializable)

### Step 4: Present the plan — STOP AND WAIT FOR CONFIRMATION

Print the full plan in this format, then ask "Proceed with generation? (yes / adjust)":

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  PLAN — <usecase name>
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

LAYER BREAKDOWN
  network/   →  route X new ActionTypes, cast to XxxDTO
  service/   →  XxxService: validate → begin tx → persist → commit
  repository/→  XxxRepository: findXxx(), saveXxx()
  model/     →  Entity A (existing), Entity B (new)
  dto/       →  XxxDTO (new), YyyDTO (existing — reuse)
  common/    →  add ActionType.XXX, YYY, ZZZ

WHAT WILL BE CREATED
  New files:
    server/model/Xxx.java
    server/dto/XxxDTO.java
    server/repository/XxxRepository.java
    server/service/XxxService.java
  Modified files:
    common/command/ActionType.java   (+3 entries)
    server/network/NetworkHandler.java (+3 cases)

WHAT WILL BE REUSED (no changes)
  server/model/Customer.java
  server/dto/CustomerDTO.java

⚠️  RISKS
  1. <risk description and suggested mitigation>
  2. <risk description and suggested mitigation>

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Proceed with generation? (yes / adjust)
```

**Do not write any code until the user replies "yes".**
If the user says "adjust", apply their feedback to the plan and show it again.

---

## Phase 2 — Generate

Only after user confirms. Generate in this order:

### 1. ActionType entries
Add to `common/command/ActionType.java` — only actions this usecase needs.
Naming: `VERB_ENTITY` or `VERB_ENTITY_BY_FIELD`

### 2. Enum (if needed)
New status or category not yet in `server/constant/`.
Include Vietnamese `getName()` method.

### 3. Entity (if new)
Follow `entity-style.md`:
- UUID PK: `@Id @GeneratedValue(strategy = GenerationType.UUID) @Column(length = 36)`
- Lombok: `@NoArgsConstructor @AllArgsConstructor @Setter @Getter @ToString @Builder`
- `@ToString.Exclude` on every relational field
- `jakarta.persistence.*` only

### 4. DTO
Follow `dto-style.md`:
- `@Data @NoArgsConstructor @AllArgsConstructor @Builder implements Serializable`
- Flat: `String parentId`, never nested DTO objects

### 5. Repository
`server/repository/<Entity>Repository.java`
- Only methods this usecase needs
- `TypedQuery` with named parameters — no string concatenation
- Method names include the entity: `findTicketById`, not `findById`

### 6. Service
`server/service/<Entity>Service.java`
- Validate input before opening transaction
- Transaction: begin → logic → commit, rollback in catch, close in finally
- Return `Response.success(...)` or `Response.error(...)`
- `rollbackQuietly(transaction)` helper for rollback

### 7. Handler (network layer)
Add cases to `server/network/`:
```java
case CREATE_TICKET -> {
    TicketDTO dto = (TicketDTO) request.getData();
    return ticketService.createTicket(dto);
}
```

### Done — print summary
```
✅ Scaffolded: <usecase>
─────────────────────────────
ActionType  → CREATE_TICKET, FIND_TICKET_BY_ID
Enum        → (none)
Entity      → Ticket.java          [new]
DTO         → TicketDTO.java       [new]
Repository  → TicketRepository.java [new]
Service     → TicketService.java   [new]
Handler     → NetworkHandler.java  [+2 cases]
```

---

## Rules

- If `docs/usecases/$ARGUMENTS.md` does not exist — stop, tell the user to create it first
- Never generate code before plan is confirmed
- Never overwrite existing files — append or modify only what is needed
- Never generate code for operations not in the usecase doc
- All generated code must follow `.claude/rules/`
- If the usecase doc is ambiguous — list exactly what is unclear, ask before proceeding
