# Serialization

All classes transmitted over the socket must implement `Serializable`. This includes every DTO, `Request`, `Response`, and any enum used as a field inside them.

## serialVersionUID

Every `Serializable` class must declare an explicit `serialVersionUID`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String customerId;
    // ...
}
```

**Why:** Without it, Java auto-generates one from the class structure. Adding a field changes the auto-generated UID → `InvalidClassException` at runtime when client and server versions differ.

**Rule:** Start at `1L`. Increment only when you make a **breaking change** (remove a field, change a field type). Adding a new optional field is non-breaking — keep the same UID.

## What can and cannot be serialized

| Serializable | Not Serializable |
|---|---|
| primitives, String, UUID | `EntityManager`, `Connection` |
| `List`, `Map`, `Set` (java.util) | Hibernate proxy objects (`@Entity`) |
| DTOs with `implements Serializable` | Lambda expressions |
| Enums | `Thread`, `Socket` |

**Never send `@Entity` objects over the socket.** Hibernate wraps entities in proxies with lazy-loaded collections — serializing them triggers `LazyInitializationException` or sends the entire object graph unexpectedly. Always convert to a DTO first.

## Backward Compatibility

When adding a new field to a DTO that is already in use:

1. Keep the same `serialVersionUID`
2. Give the field a sensible default (Lombok `@Builder.Default` or initialize in constructor)
3. Old clients that don't send the field will deserialize it as `null` — handle null defensively on the server

When removing or renaming a field → bump `serialVersionUID` to 2L. Old clients will get `InvalidClassException` — they must update.

## out.reset()

After writing each `Response` to `ObjectOutputStream`, call `out.reset()`:

```java
out.writeObject(response);
out.reset();
```

`ObjectOutputStream` caches object references by default. Without `reset()`, if the same object reference is sent again (e.g. in a loop), the stream sends a back-reference instead of the new data — the client receives the old version.
