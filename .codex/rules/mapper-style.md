# Mapper Code Style

Mappers convert between `@Entity` and `DTO`. They are the only place this conversion happens.

## Infrastructure — GenericDataMapper

Two utility classes live in `server/util/`:

```java
// Interface
public interface GenericDataMapper {
    Map<String, Object> toMap(Object object);
    <T> T toObject(Map<String, Object> data, Class<T> clazz);
}

// Implementation — owns its own ObjectMapper, configured once in the constructor
public class JacksonDataMapper implements GenericDataMapper {
    private final ObjectMapper objectMapper;

    public JacksonDataMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public Map<String, Object> toMap(Object object) { ... }

    @Override
    public <T> T toObject(Map<String, Object> data, Class<T> clazz) { ... }
}
```

- `JacksonDataMapper` is instantiated **once per entity mapper** as a private static field — never re-created per call.
- Do not share a global singleton across classes; each mapper owns its instance.

## Location

```
server/mapper/GenericDataMapper.java    ← interface
server/mapper/JacksonDataMapper.java    ← implementation
server/mapper/<Entity>Mapper.java       ← one per entity
```

Never put conversion logic in service, repository, or network layer.

## Entity Mapper Structure

```java
public class TicketMapper {

    private static final GenericDataMapper mapper = new JacksonDataMapper();

    public static TicketDTO toDto(Ticket ticket) {
        if (ticket == null) return null;
        TicketDTO dto = mapper.toObject(mapper.toMap(ticket), TicketDTO.class);
        // Manually set FK fields that differ in name between entity and DTO:
        dto.setCustomerId(ticket.getCustomer().getId());
        dto.setScheduleDetailId(ticket.getScheduleDetail().getId());
        return dto;
    }

    public static Ticket toEntity(TicketDTO ticketDTO) {
        if (ticketDTO == null) return null;
        // Relational fields (@ManyToOne, @OneToOne) must be resolved by the service.
        return mapper.toObject(mapper.toMap(ticketDTO), Ticket.class);
    }

    public static List<TicketDTO> toDtoList(List<Ticket> tickets) {
        return tickets.stream()
                .map(TicketMapper::toDto)
                .toList();
    }
}
```

## When toMap/toObject Is Not Enough

If the entity has relational fields that trigger lazy loading during serialization, annotate them with `@JsonIgnore` on the entity:

```java
@JsonIgnore
@OneToMany(mappedBy = "ticket")
private List<InvoiceDetail> invoiceDetails;
```

## Naming Rules

| Method | Purpose |
|---|---|
| `toDto(Entity)` | Entity → DTO |
| `toEntity(DTO)` | DTO → Entity (FK fields unset — service resolves them) |
| `toDtoList(List<Entity>)` | Batch conversion |

Always include the entity name: `TicketMapper`, not `Mapper`.

## Rules

- Always null-check input: `if (ticket == null) return null`
- Use `mapper.toObject(mapper.toMap(source), Target.class)` as the base conversion
- After conversion, manually set FK fields that differ in name between entity and DTO
- `toEntity()` produces a partial entity — relational fields must be set by the service, not the mapper
- Never call a repository or open a transaction inside a mapper
- Never instantiate `JacksonDataMapper` inside a method — always use the static field
- Add `@JsonIgnore` on entity relations that should not be traversed by Jackson
