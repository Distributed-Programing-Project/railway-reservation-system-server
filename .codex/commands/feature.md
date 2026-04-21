Full pipeline for implementing a usecase end-to-end.
Runs 4 phases in sequence. Each phase stops for confirmation before the next begins.

## Input

`$ARGUMENTS` — usecase name matching `docs/usecases/$ARGUMENTS.md`

Stop immediately if the file does not exist — tell the user to create it first.

---

## Phase 1 — Business Review (BA)

You are a Senior Business Analyst with 10+ years in Vietnamese railway ticketing (VR).

Read `docs/usecases/$ARGUMENTS.md` and all other files in `docs/usecases/` for consistency.
Read existing entities in `src/main/java/vn/edu/iuh/fit/server/model/`.

Evaluate:
- Is the full flow described? (happy path + alternative + error)
- Are all actors identified? (Customer, Employee, System)
- Are business rules correct for Vietnamese railway context?
  - Ticket types: Normal, Senior, Child, Student
  - 1 ticket = 1 passenger + 1 seat + 1 schedule detail
  - Round-trip = 2 separate tickets
  - Cancellation cutoff and refund rules
  - Invoice types: SALE, REFUND, EXCHANGE
  - Passenger must have CCCD or Passport
- Any conflicts with other usecases?
- Missing edge cases?

Output:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  PHASE 1 — BUSINESS REVIEW
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ What is correct: ...
🔴 Critical issues: ...
🟡 Missing pieces: ...
🔵 Suggestions: ...
```

If there are Critical issues — rewrite `docs/usecases/$ARGUMENTS.md` with corrections applied, then show the diff.

**→ Ask: "Phase 1 done. Proceed to diagram? (yes / adjust)"**
Wait for confirmation before continuing.

---

## Phase 2 — Diagram (Mermaid)

Read the (possibly updated) `docs/usecases/$ARGUMENTS.md`.

Then read **only** the files relevant to this usecase — do NOT glob the entire source tree:
- `common/command/ActionType.java` — always needed
- `common/request/Request.java`, `common/response/Response.java` — always needed
- Entity files mentioned in the usecase doc (e.g. `server/model/Ticket.java`)
- DTO files mentioned in the usecase doc (e.g. `server/dto/TicketDTO.java`)
- Existing mapper for those entities (`server/mapper/<Entity>Mapper.java`) if it exists
- Existing service/repository for those entities if they exist

Generate 3 diagrams and save to `docs/diagrams/sequence/$ARGUMENTS.md`:

1. **System Architecture** — full distributed layers (Client → TCP Socket → Server → JPA → MariaDB), must show `ObjectOutputStream/ObjectInputStream`, `Request`/`Response`, `ActionType`
2. **Sequence diagram** — full request-response cycle for this usecase, label every arrow with actual class/method names
3. **Class diagram** — only entities and DTOs involved, mark `<<entity>>` and `<<DTO>>`, show relationships

**→ Ask: "Phase 2 done. Proceed to scaffold plan? (yes / adjust)"**
Wait for confirmation before continuing.

---

## Phase 3 — Scaffold (Plan → Confirm → Generate)

Read **only** the files relevant to this usecase in parallel — do NOT glob the entire source tree:
- `docs/usecases/$ARGUMENTS.md` (re-read if Phase 1 modified it)
- `common/command/ActionType.java` — to know which ActionTypes already exist
- `server/network/RequestRouter.java` — to know which routes already exist
- Entity files mentioned in the usecase doc
- DTO files mentioned in the usecase doc
- Existing mapper/repository/service for those entities (to avoid duplicating code)
- Any enum files referenced in the usecase doc (`server/constant/`)

Identify what already exists vs what needs to be created before writing the plan.
**CRITICAL RULE:** Pay special attention to the `## 🛠 Yêu cầu cập nhật Database / Entity / DTO (Từ BA Review)` section at the very bottom of the usecase document. Your plan and generated code MUST implement all of these BA requirements.

### 3a — Plan

Present the full implementation plan:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  PHASE 3 — SCAFFOLD PLAN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

LAYER BREAKDOWN
  network/    → route X new ActionTypes, cast to XxxDTO
  service/    → XxxService: validate → begin tx → persist → commit
  repository/ → XxxRepository: findXxx(), saveXxx()
  model/      → Entity A (existing), Entity B (new)
  dto/        → XxxDTO (new), YyyDTO (existing — reuse)
  common/     → add ActionType.XXX, YYY

WHAT WILL BE CREATED
  New:      server/model/Xxx.java
            server/dto/XxxDTO.java
            server/repository/XxxRepository.java
            server/service/XxxService.java
  Modified: common/command/ActionType.java  (+3 entries)
            server/network/NetworkHandler.java (+3 cases)

WHAT WILL BE REUSED
  server/model/Customer.java
  server/dto/CustomerDTO.java

⚠️  RISKS
  1. <risk + mitigation>
```

**→ Ask: "Scaffold plan ready. Proceed to generate code? (yes / adjust)"**
Wait for confirmation before generating.

### 3b — Generate

Only after confirmed. Generate in order:
1. ActionType entries (only what this usecase needs)
2. Enum (if new)
3. Entity (if new) — follow `entity-style.md`. 
    - **CRITICAL:** ALWAYS use `@ToString.Exclude` on all `@OneToMany`, `@ManyToOne`, and `@OneToOne` fields to prevent N+1 queries or StackOverflow during logging/toString.
4. DTO — follow `dto-style.md`
5. Repository — `TypedQuery`, named params, entity in method names.
   - **CRITICAL:** Repository MUST ONLY interact with `Entity` objects. Never pass DTOs into or return DTOs from the Repository.
   - **CRITICAL:** ALL Repositories MUST extend `AbstractGenericRepositoryImpl<Entity, ID>`.
   - **CRITICAL:** NEVER use raw `try (var em = JPAUtils...)` or `em.getTransaction().begin()`. ALWAYS use `doInTransaction(em -> { ... })` for INSERT/UPDATE/DELETE, and `doWithEntityManager(em -> { ... })` for SELECT.
6. Service — transaction boundary, bean validation, `rollbackQuietly`, `Response.success/error`
   - **CRITICAL:** Service methods MUST ALWAYS call `ValidationUtils.validate(dto)` at the very beginning. Stop and return `Response.error` if validation fails.
   - **CRITICAL:** Service methods MUST ONLY receive `DTO`s (or primitives) from the Network layer, NEVER Entities.
   - **CRITICAL:** Service methods MUST ALWAYS return a `Response` object (e.g., `Response.success(dto)`), NEVER return Entities or raw DTOs.
   - **CRITICAL:** Instantiate Repositories as `private final` class fields, NEVER inside a method.
   - **CRITICAL:** Use proper `import` statements at the top of the file, NEVER use fully-qualified class names inline.
   - **CRITICAL:** Use Mapper classes (following `mapper-style.md`) for Entity↔DTO conversions, NEVER map manually inside the service.
7. Handler — switch cases in network layer

Never overwrite existing files. Never generate code not in the usecase doc.

**→ Ask: "Code generated. Proceed to code review? (yes / skip)"**
Wait for confirmation before continuing.

---

## Phase 4 — Code Review (Tech Lead)

You are a senior Java tech lead. Review all files generated in Phase 3.

Check:
1. Architecture & layer violations
2. Naming
   - **CRITICAL:** Never use generic plural names like `dtos`, `entities`, `list`, or `data`. Variable names must explicitly include the domain model name (e.g., `scheduleDTOList`, `ticketDTOs`).
   - Entity in every method/variable name, no single letters (e.g., use `Schedule s` only in short lambdas, otherwise `schedule`).
3. Entity code style (UUID PK, Lombok order, `@ToString.Exclude`, `jakarta.persistence`)
4. DTO code style (`Serializable`, flat, `@Data`)
5. Distributed system concerns (no `@Entity` over socket, null checks on cast)
6. JPA / Hibernate (N+1, missing `mappedBy`, fetch types)
   - **CRITICAL:** Check for N+1 issues. Ensure `JOIN FETCH` is used in JPQL when accessing relationships in a list.
   - **CRITICAL:** Ensure `@ToString.Exclude` is present on ALL relationship fields.
   - **CRITICAL:** Check if Repository extends `AbstractGenericRepositoryImpl` and uses `doInTransaction` or `doWithEntityManager`. Reject any raw `em.getTransaction()` code.
7. Code quality (method length, duplication, no unnecessary comments)
8. Code simplification (show before/after for anything reducible)
9. Security (no JPQL string concat, no hardcoded credentials)

Output findings by severity: 🔴 Blocker / 🟡 Must Fix / 🔵 Suggestion / ✅ Looks Good

If there are Blocker or Must Fix issues — fix them immediately, then show what changed.

---

## Final Summary

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  DONE — <usecase>
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Phase 1  Business Review   ✅
Phase 2  Diagram           ✅  docs/diagrams/$ARGUMENTS.md
Phase 3  Scaffold          ✅  7 files created/modified
Phase 4  Code Review       ✅  APPROVE / REQUEST CHANGES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Run /commit to commit this usecase.
```

## Rules

- Never skip a phase without explicit user confirmation
- Never generate code before Phase 3 plan is confirmed
- All code must follow `.claude/rules/`
- If usecase doc is ambiguous at any phase — stop and ask
