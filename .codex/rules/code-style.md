# Code Style Guidelines

## Keep Methods Short — Extract Helpers

Extract repeated or logically distinct code into private helpers. Never copy-paste the same block.

- Same logic in 2+ places → extract immediately.
- Method > ~20 lines → consider splitting.
- Helpers shared across services → `server/util/`.
- DTO/entity conversion → `server/mapper/`.

**Bad:**
```java
public Response handle(Request req) {
    Object raw = req.getData();
    if (raw == null) return Response.error("Missing data");
    TicketDTO dto = (TicketDTO) raw;
    if (dto.getCustomerId() == null) return Response.error("Missing customer");
    // ... 30 more lines
}
```

**Good:**
```java
public Response handle(Request req) {
    TicketDTO dto = extractDto(req, TicketDTO.class);
    validate(dto);
    return ticketService.create(dto);
}

private <T> T extractDto(Request req, Class<T> type) {
    if (req.getData() == null) throw new IllegalArgumentException("Missing data");
    return type.cast(req.getData());
}

private void validate(TicketDTO dto) {
    if (dto.getCustomerId() == null) throw new IllegalArgumentException("Missing customer");
}
```

## Naming — Always Include the Entity

Method and variable names must be **self-contained** — never use a bare verb or a single letter.

- Include the entity/subject in the name so it reads clearly without context
- No single-letter variables (`t`, `e`, `s`) except loop counters (`i`, `j`)

| Bad | Good |
|---|---|
| `create(dto)` | `createTicket(dto)` |
| `find(id)` | `findInvoiceById(id)` |
| `delete(id)` | `deleteCustomer(id)` |
| `TicketDTO t` | `TicketDTO ticketDTO` |
| `List<Ticket> list` | `List<Ticket> tickets` |
| `String s` | `String customerId` |

This applies to: method names, parameter names, local variables, and field names.

---

## Comments

Default to **no comments**. Only add one when the WHY is non-obvious: a hidden constraint, a workaround, a subtle invariant. Never describe what the code does — well-named identifiers do that.

## No Over-engineering

Don't add features, abstractions, or error handling beyond what the task requires. Three similar lines is better than a premature abstraction.

## No Unnecessary Files

Never create a file that was not explicitly requested or required by the task.

- No utility classes, helper files, test stubs, or config files "just in case"
- No placeholder files, `text.txt`, `TODO.md`, or scratch files
- If a new file is genuinely needed, state why before creating it
- When in doubt — don't create it, ask first

## Asynchronous & Concurrent Code

This is a distributed system — every socket connection runs on its own thread. Blocking the main thread or sharing mutable state across threads causes deadlocks and race conditions.

**Rules:**
- Each client connection must be handled on a **separate thread** — never block the main server thread
- Use `CompletableFuture` or `ExecutorService` for operations that can run in parallel (e.g. querying DB while preparing response)
- Never use `Thread.sleep()` as a synchronization mechanism — use proper locks or futures
- Shared mutable state (e.g. a cache, a counter) must be protected with `synchronized`, `AtomicXxx`, or `ConcurrentXxx` — never raw field access across threads
- DB operations are blocking by nature — keep them off the socket I/O thread; delegate to a thread pool

**Pattern for each client connection:**
```java
// Good — each connection gets its own thread
ExecutorService threadPool = Executors.newCachedThreadPool();

serverSocket.accept(); // in a loop
threadPool.submit(() -> handleClient(clientSocket));

// Bad — sequential, blocks the next connection
while (true) {
    Socket client = serverSocket.accept();
    handleClient(client); // blocks until this client is done
}
```

**CompletableFuture for parallel work:**
```java
// Good — DB query and response preparation in parallel
CompletableFuture<List<Ticket>> ticketsFuture =
    CompletableFuture.supplyAsync(() -> ticketRepository.findAllTickets());

CompletableFuture<List<Invoice>> invoicesFuture =
    CompletableFuture.supplyAsync(() -> invoiceRepository.findAllInvoices());

CompletableFuture.allOf(ticketsFuture, invoicesFuture).join();
```

## Actively Simplify Long Methods

If a method can be made significantly shorter without losing clarity — rewrite it, don't leave it.

- A 200-line method reducible to 50 lines → rewrite it to 50 lines
- A 50-line method reducible to 15 lines → rewrite it to 15 lines
- Simplification is not optional when the opportunity is obvious

Tools to reach for:
- Early return / guard clause instead of nested `if/else`
- Stream + lambda instead of imperative loops
- Lombok annotations to eliminate boilerplate
- Extract private helpers for repeated or distinct sub-steps
- Collapse redundant null checks into a single guard at the top
