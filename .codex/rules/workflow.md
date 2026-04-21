# Development Workflow

Work **vertically** (one full feature top-to-bottom) not per-layer:

1. Add `ActionType` constant in `common/command/ActionType.java`
2. Add or reuse DTO in `server/dto/`
3. Add enum in `server/constant/` if needed
4. Implement `model` → `repository` → `service` in `server`
5. Wire routing in `server/network` (switch on `ActionType`)
6. Implement UI + socket call in `client` (when client is added)

---

## Key Dependencies

| Dependency | Version |
|---|---|
| hibernate-core | 7.0.4.Final |
| mariadb-java-client | 3.5.7 |
| lombok | 1.18.42 |
| jackson-databind | 3.1.0 |
| jackson-datatype-jsr310 | 2.21.1 |
| jaxb-runtime | 4.0.5 |
| junit-jupiter | 5.11.0 |
