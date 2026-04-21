# Logging

## What to Log at Each Layer

| Layer | Level | What to log |
|---|---|---|
| `network/` | `INFO` | Client connected/disconnected (IP + port) |
| `network/` | `DEBUG` | Incoming `ActionType` per request |
| `network/` | `ERROR` | Unexpected exceptions during socket handling |
| `service/` | `DEBUG` | Method entry with key parameters (no sensitive data) |
| `service/` | `WARN` | Business rule violations (e.g. ticket already cancelled) |
| `service/` | `ERROR` | Exceptions caught before returning `Response.error` |
| `repository/` | `DEBUG` | Slow queries (> 500ms) |

## What NOT to Log

- Passwords, tokens, session keys — ever
- Full credit card numbers, passport numbers, CCCD numbers
- Full entity objects that contain sensitive fields
- Stack traces at `INFO` or `WARN` level — stack traces go in `ERROR` only

## Logger Setup (SLF4J)

Use SLF4J with a concrete binding (Logback or Log4j2). Declare one logger per class:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    public Response createTicket(TicketDTO ticketDTO) {
        log.debug("createTicket called: customerId={}", ticketDTO.getCustomerId());
        // ...
        log.info("Ticket created successfully: ticketId={}", savedTicket.getId());
    }
}
```

## Parameterized Logging

Always use parameterized log statements — never string concatenation:

```java
// Bad — string is built even if DEBUG is disabled
log.debug("Creating ticket for customer: " + ticketDTO.getCustomerId());

// Good — string only built if DEBUG is enabled
log.debug("Creating ticket for customer: {}", ticketDTO.getCustomerId());
```

## Client Lifecycle Logging

Log every connect and disconnect at `INFO` — this is the audit trail of the distributed system:

```java
log.info("Client connected: {}", clientSocket.getRemoteSocketAddress());
// ... handle client ...
log.info("Client disconnected: {}", clientSocket.getRemoteSocketAddress());
```

## Error Logging with Context

When logging an error before returning `Response.error`, include the operation context:

```java
catch (PersistenceException e) {
    log.error("Failed to create ticket: customerId={}, scheduleDetailId={}",
        ticketDTO.getCustomerId(), ticketDTO.getScheduleDetailId(), e);
    return Response.error("Database error saving ticket");
}
```

The exception object (`e`) as the last parameter causes SLF4J to append the full stack trace automatically — no need to call `e.getMessage()` or `e.printStackTrace()`.
