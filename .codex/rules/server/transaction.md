# Transaction Management

## Boundary Rule

Transactions belong in the **service layer** only — never in repository or network layer.

```
network layer   → no transaction
service layer   → transaction starts and ends here ✓
repository layer → runs inside the caller's transaction
```

```java
// Good — transaction in service
public class TicketService {
    public Response createTicket(TicketDTO ticketDTO) {
        EntityManager entityManager = JPAUtils.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            // ... business logic
            transaction.commit();
            return Response.success("Ticket created", ticketDTO);
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            return Response.error("Failed to create ticket: " + e.getMessage());
        } finally {
            entityManager.close();
        }
    }
}
```

## Always Close EntityManager

`EntityManager` is not thread-safe. Create a new one per request, close it in `finally`.

```java
EntityManager entityManager = JPAUtils.getEntityManager();
try {
    // use it
} finally {
    if (entityManager.isOpen()) entityManager.close();
}
```

Never inject or share a single `EntityManager` across threads.

## Rollback Rules

- Any `RuntimeException` during a transaction → rollback
- Never swallow exceptions silently inside a transaction — either rollback or rethrow
- After rollback, always return `Response.error(...)` — never return success

## Optimistic Locking

For entities that can be updated concurrently, add a `@Version` field:

```java
@Version
@Column(name = "version")
private int version;
```

When two threads update the same row simultaneously, Hibernate throws `OptimisticLockException` on the second commit. Catch it in the service and return a meaningful error:

```java
catch (OptimisticLockException e) {
    transaction.rollback();
    return Response.error("Data was modified by another user. Please retry.");
}
```

## Don't Hold Transactions Open Across Socket Calls

Never start a transaction before sending data to the client and commit after receiving the response. The socket round-trip can take seconds — holding a DB transaction that long locks rows and starves other threads.

Pattern to avoid:
```java
// Bad — transaction spans a socket round-trip
transaction.begin();
out.writeObject(someData);       // waiting for client...
Request confirmation = in.readObject(); // could block forever
transaction.commit();
```

Each `Request` → `Response` cycle must be one self-contained transaction.
