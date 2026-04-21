# DTO Code Style

DTOs use `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor` + `@Builder`. Must implement `Serializable` for socket transmission.

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExampleDTO implements Serializable {
    private String id;
    private String name;
    private SomeType type;
    private String parentId;   // flat FK — never embed the full parent DTO
}
```

## Rules

- One DTO per entity — reuse for create / update / fetch / display.
- **Flat:** reference parent by `String parentId`, never nest the full parent object.
- Only add a specialized DTO when fields genuinely differ (e.g. `LoginRequestDTO`).
- `LocalDateTime` fields: annotate with `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`.

## Location (temporary)

Currently in `server/dto/`. Will move to `common/dto/` before the repo split so both client and server share them via Maven dependency.
