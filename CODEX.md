# CODEX.md — Train Booking System

## Project Overview

Distributed desktop application using a Client–Server model over TCP Socket.

| Layer         | Technology                     |
| ------------- | ------------------------------ |
| Client UI     | JavaFX                         |
| Communication | Socket TCP (Java ObjectStream) |
| Backend       | Java 21 + Socket Server        |
| Persistence   | JPA / Hibernate 7              |
| Database      | MariaDB                        |
| Build         | Maven                          |
| Utilities     | Lombok, Jackson                |

---

## Usecase Documentation

Mỗi usecase được tài liệu hóa bằng Mermaid diagram, sinh ra bởi lệnh `@mermaid.md <usecase>`.

```
docs/
├── diagrams/
│ └── sequence/
│ ├── login.md # Sequence + class diagram cho usecase đăng nhập
│ ├── buy-ticket.md # Sequence + class diagram cho usecase mua vé
│ ├── create-invoice.md # Sequence + class diagram cho usecase tạo hóa đơn
│ └── ... # Thêm mới bằng @mermaid.md <usecase>
└── usecases/
├── login.md # Mô tả nghiệp vụ usecase đăng nhập (viết tay)
├── buy-ticket.md # Mô tả nghiệp vụ usecase mua vé (viết tay)
└── ... # @scaffold.md <usecase> đọc file này để sinh code
```

Mỗi file trong `diagrams/` gồm 3 diagram: **System Architecture** (toàn bộ luồng phân tán), **Sequence** (flow chi tiết của usecase), **Class Diagram** (entity + DTO liên quan).

Mỗi file trong `usecases/` mô tả nghiệp vụ bằng ngôn ngữ tự nhiên — actor, luồng chính, validation, dữ liệu vào/ra. `/scaffold <usecase>` đọc file này để sinh code đúng với nghiệp vụ.

---

## Rules & Guidelines

```
.claude/
├── commands/              # Executable AI skills (e.g., feature.md, commit.md)
│   ├── feature.md        # /feature <usecase>  ← full pipeline
│   ├── reviewusecase.md  # /reviewusecase <name> <mô tả>
│   ├── bizreview.md      # /bizreview <usecase>
│   ├── scaffold.md       # /scaffold <usecase>
│   ├── mermaid.md        # /mermaid <usecase>
│   ├── reviewcode.md     # /reviewcode
│   ├── reviewarch.md     # /reviewarch
│   ├── commit.md         # /commit
│   └── merge-main.md     # /merge-main
└── rules/                 # Technical standards AI must strictly follow
    ├── client/            # JavaFX & UI rules
    │   └── client-style.md   # JavaFX guidelines, DTO binding, Socket request flow
    ├── design-pattern/    # System architecture and design patterns
    ├── server/            # JPA, Service, Mapping rules
    │   ├── entity-style.md   # Entity conventions + UUID primary keys
    │   ├── jpa-performance.md# N+1, JOIN FETCH, pagination, LAZY/EAGER
    │   ├── transaction.md    # Boundary rules, rollback, optimistic lock
    │   └── mapper-style.md   # Entity ↔ DTO conversion, static methods, naming
    └── common/
        ├── architecture.md   # Monorepo strategy, module structure, ERD, DB config
        ├── protocol.md       # Request/Response, ActionType, socket flow
        ├── dto-style.md      # DTO rules + Serializable
        ├── code-style.md     # Naming, async, simplification, no extra files
        ├── workflow.md       # Dev workflow, dependencies
        ├── socket-server.md  # Thread pool, timeout, heartbeat, out.reset()
        ├── serialization.md  # serialVersionUID, versioning, backward compat
        ├── error-handling.md # Exception hierarchy, rollback helper, validation
        ├── validation.md     # Bean Validation (JSR 303), DTO constraints, manual trigger
        ├── logging.md        # Log levels, SLF4J, sensitive data, parameterized
        └── network-style.md  # Server, RequestRouter, switch pattern, castData
```

---

@.codex/rules/client/client-style.md
@.codex/rules/server/entity-style.md
@.codex/rules/server/jpa-performance.md
@.codex/rules/server/transaction.md
@.codex/rules/server/mapper-style.md
@.codex/rules/common/architecture.md
@.codex/rules/common/protocol.md
@.codex/rules/common/dto-style.md
@.codex/rules/common/code-style.md
@.codex/rules/common/workflow.md
@.codex/rules/common/socket-server.md
@.codex/rules/common/serialization.md
@.codex/rules/common/error-handling.md
@.codex/rules/common/logging.md
@.codex/rules/common/network-style.md
