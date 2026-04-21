# Entity Code Style

- Imports: `jakarta.persistence.*` (Hibernate 7 — **not** `javax.persistence`)
- Lombok order: `@NoArgsConstructor`, `@AllArgsConstructor`, `@Setter`, `@Getter`, `@ToString`, `@Builder`
- Always `@ToString(exclude = {...})` on any relational field to avoid infinite recursion
- UUID primary keys: `@Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "...", length = 36)`
- Enums: `@Enumerated(EnumType.STRING)`
- Vietnamese text columns: `columnDefinition = "NVARCHAR(255)"`
- Column names: `snake_case`

```java
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString(exclude = {"relatedField"})
@Builder
@Entity
@Table(name = "table_name")
public class ExampleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "example_id", length = 36)
    private String id;

    @Column(name = "full_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SomeType type;

    @ManyToOne
    @JoinColumn(name = "other_id")
    @ToString.Exclude
    private OtherEntity other;

    @OneToMany(mappedBy = "example")
    @ToString.Exclude
    private List<ChildEntity> children;
}
```
