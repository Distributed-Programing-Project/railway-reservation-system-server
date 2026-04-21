# Error Handling

## Response Is the Error Boundary

The socket layer returns `Response` objects — never let a raw exception reach the client. Every exception must be caught at the service level and converted to `Response.error(message)`.

```java
// Good — exception caught, client gets a clean message
public Response createTicket(TicketDTO ticketDTO) {
    EntityManager entityManager = JPAUtils.getEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    try {
        transaction.begin();
        // ...
        transaction.commit();
        return Response.success("Ticket created successfully", ticketDTO);
    } catch (OptimisticLockException e) {
        rollbackQuietly(transaction);
        return Response.error("Data conflict — please retry");
    } catch (PersistenceException e) {
        rollbackQuietly(transaction);
        return Response.error("Database error: " + e.getMessage());
    } catch (Exception e) {
        rollbackQuietly(transaction);
        return Response.error("Unexpected error: " + e.getMessage());
    } finally {
        entityManager.close();
    }
}
```

## Exception Hierarchy to Handle

| Exception | Layer | Meaning | Action |
|---|---|---|---|
| `EOFException` | network | client disconnected | log info, close socket |
| `SocketException` | network | network error | log info, close socket |
| `SocketTimeoutException` | network | client timed out | log warn, close socket |
| `ClassNotFoundException` | network | DTO class mismatch between client/server | log error, return error response |
| `ClassCastException` | network | wrong DTO type sent for ActionType | return `Response.error` |
| `OptimisticLockException` | service | concurrent update conflict | rollback, return retry message |
| `PersistenceException` | service | JPA/DB error | rollback, return error message |
| `IllegalArgumentException` | service | invalid input (null, bad value) | return validation error |

## Rollback Helper

Extract rollback into a helper to avoid repeating the `isActive()` check:

```java
private void rollbackQuietly(EntityTransaction transaction) {
    if (transaction != null && transaction.isActive()) {
        transaction.rollback();
    }
}
```

## Error Messages

- Be specific enough for debugging: `"Customer not found: id=abc123"` not `"Error"`
- Don't expose stack traces or internal class names to the client
- Don't expose SQL errors directly — wrap them: `"Database error saving ticket"` not the raw SQL message

## Validation Before Persistence

Validate input at the top of the service method, before opening a transaction:

```java
public Response createTicket(TicketDTO ticketDTO) {
    if (ticketDTO == null) return Response.error("Ticket data is required");
    if (ticketDTO.getCustomerId() == null) return Response.error("Customer ID is required");
    if (ticketDTO.getScheduleDetailId() == null) return Response.error("Schedule detail is required");

    // open transaction only after validation passes
    EntityManager entityManager = JPAUtils.getEntityManager();
    // ...
}
```

## Never Swallow Exceptions

```java
// Bad
try {
    transaction.commit();
} catch (Exception e) {
    // silent — nobody knows this failed
}

// Good
try {
    transaction.commit();
} catch (Exception e) {
    rollbackQuietly(transaction);
    return Response.error("Failed to save: " + e.getMessage());
}
```
