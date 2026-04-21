# JPA Performance

## Default Fetch Types

| Relationship | Default | Recommended |
|---|---|---|
| `@ManyToOne` | `EAGER` | keep EAGER (single join, safe) |
| `@OneToOne` | `EAGER` | keep EAGER |
| `@OneToMany` | `LAZY` | keep LAZY — never change to EAGER |
| `@ManyToMany` | `LAZY` | keep LAZY |

Never set `@OneToMany(fetch = FetchType.EAGER)` — it loads the entire child collection for every parent query, regardless of whether you need it.

## N+1 Problem

N+1 happens when you load N entities and then trigger 1 extra query per entity to load a relationship.

```java
// Bad — N+1: 1 query for all tickets + 1 query per ticket to load customer
List<Ticket> tickets = entityManager
    .createQuery("SELECT t FROM Ticket t", Ticket.class)
    .getResultList();

tickets.forEach(ticket -> System.out.println(ticket.getCustomer().getName())); // N extra queries
```

```java
// Good — 1 query with JOIN FETCH
List<Ticket> tickets = entityManager
    .createQuery("SELECT t FROM Ticket t JOIN FETCH t.customer", Ticket.class)
    .getResultList();
```

Use `JOIN FETCH` in JPQL whenever you know you will access the related entity.

### The toString() Trap

Never include relationship fields in `toString()` (especially lazy ones). Lombok's default `@ToString` will call `getter` methods on all fields, triggering a database query for every lazy relationship for EVERY entity in a list.

**CRITICAL:** Always use `@ToString.Exclude` on relationship fields:
```java
@Entity
@ToString // Lombok
public class Ticket {
    // ...
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude // Prevent N+1 when logging or printing the entity
    private Customer customer;
}
```

## Pagination

Never load an unbounded result set. Always paginate:

```java
List<Ticket> tickets = entityManager
    .createQuery("SELECT t FROM Ticket t ORDER BY t.id", Ticket.class)
    .setFirstResult(page * pageSize)   // offset
    .setMaxResults(pageSize)           // limit
    .getResultList();
```

For count queries (total pages):
```java
Long total = entityManager
    .createQuery("SELECT COUNT(t) FROM Ticket t", Long.class)
    .getSingleResult();
```

## Named Queries vs Inline JPQL

For queries used more than once, define them as `@NamedQuery` on the entity:

```java
@NamedQuery(name = "Ticket.findByCustomerId",
            query = "SELECT t FROM Ticket t WHERE t.customer.id = :customerId")
```

For one-off or dynamic queries, inline JPQL is fine.

## Never Use String Concatenation in JPQL

```java
// Bad — JPQL injection risk + no type safety
String jpql = "SELECT t FROM Ticket t WHERE t.status = '" + status + "'";

// Good — named parameter
TypedQuery<Ticket> query = entityManager
    .createQuery("SELECT t FROM Ticket t WHERE t.status = :status", Ticket.class)
    .setParameter("status", status);
```

## Convert to DTO Before Sending

After fetching entities, always map to DTO before returning through the socket. Never return a raw `@Entity` — lazy collections will fail to serialize.

```java
// Good
List<TicketDTO> ticketDTOs = tickets.stream()
    .map(ticketMapper::toDto)
    .toList();
return Response.success("OK", ticketDTOs);
```
