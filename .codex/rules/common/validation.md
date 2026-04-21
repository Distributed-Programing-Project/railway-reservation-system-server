# Bean Validation Standard (JSR 303/380)

All input data coming from the Network layer must be validated using Jakarta Bean Validation annotations.

## 1. DTO Annotations
Place validation constraints directly on DTO fields. This ensures consistent validation logic regardless of which service consumes the DTO.

### Common Annotations:
- `@NotBlank(message = "...")`: For non-null, non-empty Strings.
- `@NotNull(message = "...")`: For non-null Objects (Dates, Numbers, Enums).
- `@Email(message = "...")`: For valid email formats.
- `@Pattern(regexp = "...", message = "...")`: For custom regex (CCCD, Phone, etc.).
- `@Min`/`@Max`: For numeric ranges.
- `@Future`/`@Past`: For date constraints.

```java
public class ExampleDTO implements Serializable {
    @NotBlank(message = "Name is required")
    private String name;
    
    @Email(message = "Invalid email")
    private String email;
}
```

## 2. Service Layer Execution
Since the project does not use an automatic injection framework (like Spring `@Valid`), validation must be triggered manually at the start of every Service method using a centralized utility.

### Standard Pattern:
```java
public Response someServiceMethod(SomeDTO dto) {
    // 1. Centralized Validation
    List<String> errors = ValidationUtils.validate(dto);
    if (!errors.isEmpty()) {
        return Response.error(String.join(", ", errors));
    }
    
    // 2. Business Logic only if validation passes
    // ...
}
```

## 3. Business Rule Validation vs. Input Validation
- **Input Validation (Bean Validation):** Format, null-checks, ranges. Handled in DTO + Utility.
- **Business Rule Validation:** Cross-entity checks, database existence, logical constraints. Handled manually in Service layer (e.g., `if (repository.existsByEmail(...))`).

## 4. Implementation Requirement
Any new DTO created MUST include validation annotations for all mandatory or formatted fields. Failure to include these annotations is a blocker in code review.
