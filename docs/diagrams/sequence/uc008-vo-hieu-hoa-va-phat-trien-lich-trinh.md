# Diagrams — UC008: Vô hiệu hoá và Phát triển lịch trình tàu

---

## 1. System Architecture

```mermaid
flowchart TB
    subgraph Client ["🖥️ Client (Java Swing / JavaFX)"]
        UI[("Màn hình\nQuản lý lịch trình")]
    end

    subgraph SocketLayer ["🔌 TCP Socket Layer"]
        OOS["ObjectOutputStream"]
        OIS["ObjectInputStream"]
    end

    subgraph NetworkLayer ["🌐 Network Layer"]
        RH["NetworkHandler\n.handleConnection()"]
        RR["RequestRouter.route()"]
    end

    subgraph ServiceLayer ["⚙️ Service Layer"]
        SS["ScheduleServiceImpl"]
        ES["EmployeeServiceImpl"]
    end

    subgraph RepositoryLayer ["💾 Repository Layer"]
        SR["ScheduleRepositoryImpl"]
        TR["TicketRepository"]
        SDR["ScheduleDetailRepository"]
        ER["EmployeeRepositoryImpl"]
    end

    subgraph Persistence ["🗄️ JPA / Hibernate"]
        EMF["EntityManagerFactory\n(Hibernate)"]
    end

    subgraph Database ["🗃️ MariaDB"]
        SCH["schedules"]
        SD["schedule_details"]
        TKT["tickets"]
        EMP["employees"]
    end

    UI --> OOS
    OOS --> OIS
    OIS -->|Request [action, data]| RR
    RR -->|ScheduleLifecycleDTO| SS
    SS -->|findScheduleById| SR
    SS -->|findSoldSeatCount| SDR
    SS -->|findEmployeeById| ER
    SR -->|SELECT / UPDATE| EMF
    SDR -->|SELECT COUNT| EMF
    ER -->|SELECT| EMF
    EMF --> SCH
    EMF --> SD
    EMF --> TKT
    EMF --> EMP
```

---

## 2. Sequence Diagram

### Luồng DISABLE (Vô hiệu hoá)

```mermaid
sequenceDiagram
    participant User as 👤 Nhân viên quản lý
    participant UI as 🖥️ Client (Swing/FX)
    participant OIS as ObjectInputStream
    participant RR as RequestRouter
    participant SS as ScheduleServiceImpl
    participant SR as ScheduleRepositoryImpl
    participant SDR as ScheduleDetailRepositoryImpl
    participant ER as EmployeeRepositoryImpl
    participant DB as MariaDB

    User->>UI: Chọn "Vô hiệu hóa" lịch trình
    UI->>UI: Xác nhận hành động
    UI->>OIS: Request(PUBLISH_OR_DISABLE_SCHEDULE, ScheduleLifecycleDTO)
    OIS->>RR: route(request)
    RR->>SS: disableSchedule(dto)

    Note over SS: ValidationUtils.validate(dto)
    SS->>ER: findEmployeeById(dto.requestEmployeeId)
    ER-->>SS: Employee (or null)

    alt Employee not found or not Manager
        SS-->>RR: Response.error(UNAUTHORIZED)
        RR-->>UI: Response.error
        UI-->>User: Hiển thị lỗi "Không có quyền"
    end

    SS->>SR: findScheduleById(em, dto.scheduleId)
    SR-->>SS: Schedule

    alt Schedule not found
        SS-->>RR: Response.error(SCHEDULE_NOT_FOUND)
        RR-->>UI: Response.error
        UI-->>User: Hiển thị lỗi "Không tìm thấy lịch trình"
    end

    alt schedule.status == DRAFT
        Note over SS: Xóa hẳn lịch trình nháp
        SS->>SR: deleteSchedule(em, scheduleId)
        Note over SR: DELETE FROM schedule_details<br/>WHERE schedule_id = ?
        Note over SR: DELETE FROM schedules<br/>WHERE schedule_id = ?
        SR-->>SS: true
        SS-->>RR: Response.success(DISABLE_SUCCESS)
    else schedule.status == NOT_STARTED
        Note over SS: Kiểm tra số vé đã bán
        SS->>SDR: countSoldSeatsByScheduleId(em, scheduleId)
        SDR-->>SS: soldCount: long

        alt soldCount > 0
            Note over SS: Đã có khách mua vé → Từ chối
            SS-->>RR: Response.error(TICKETS_SOLD_BLOCKED)
            RR-->>UI: Response.error
            UI-->>User: Hiển thị lỗi "Đã có vé được bán"
        else soldCount == 0
            Note over SS: Chưa có ai mua → PAUSED
            SS->>SR: updateStatus(em, scheduleId, PAUSED)
            SR-->>SS: true
            SS-->>RR: Response.success(DISABLE_SUCCESS)
        end
    else schedule.status != DRAFT, NOT_STARTED
        SS-->>RR: Response.error(WRONG_STATUS_DISABLE)
        RR-->>UI: Response.error
        UI-->>User: Hiển thị lỗi "Sai trạng thái"
    end

    RR-->>UI: Response
    UI-->>User: Thông báo kết quả & làm mới danh sách
```

### Luồng PUBLISH (Phát triển)

```mermaid
sequenceDiagram
    participant User as 👤 Nhân viên quản lý
    participant UI as 🖥️ Client (Swing/FX)
    participant OIS as ObjectInputStream
    participant RR as RequestRouter
    participant SS as ScheduleServiceImpl
    participant SR as ScheduleRepositoryImpl
    participant SDR as ScheduleDetailRepositoryImpl
    participant ER as EmployeeRepositoryImpl
    participant DB as MariaDB

    User->>UI: Chọn "Phát triển" lịch trình
    UI->>UI: Xác nhận hành động
    UI->>OIS: Request(PUBLISH_OR_DISABLE_SCHEDULE, ScheduleLifecycleDTO)
    OIS->>RR: route(request)
    RR->>SS: publishSchedule(dto)

    Note over SS: ValidationUtils.validate(dto)
    SS->>ER: findEmployeeById(dto.requestEmployeeId)
    ER-->>SS: Employee

    alt Employee not Manager
        SS-->>RR: Response.error(UNAUTHORIZED)
        RR-->>UI: Response.error
        UI-->>User: Hiển thị lỗi "Không có quyền"
    end

    SS->>SR: findScheduleById(em, dto.scheduleId)
    SR-->>SS: Schedule

    alt Schedule not DRAFT
        SS-->>RR: Response.error(ONLY_DRAFT_CAN_BE_PUBLISHED)
        RR-->>UI: Response.error
        UI-->>User: Hiển thị lỗi "Chỉ nháp mới phát triển được"
    end

    Note over SS: Kiểm tra tất cả priceSeat > 0
    SS->>SDR: existsUnpricedSeat(em, scheduleId)
    SDR-->>SS: hasUnpricedSeats: boolean

    alt hasUnpricedSeats == true
        Note over SS: Còn ghế giá 0 → Chặn
        SS-->>RR: Response.error(PRICE_NOT_CONFIGURED)
        RR-->>UI: Response.error
        UI-->>User: Hiển thị lỗi "Chưa cấu hình giá vé"
    else hasUnpricedSeats == false
        SS->>SR: updateStatus(em, scheduleId, NOT_STARTED)
        SR-->>SS: true
        SS-->>RR: Response.success(PUBLISH_SUCCESS)
    end

    RR-->>UI: Response
    UI-->>User: Thông báo thành công & làm mới danh sách
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class Request {
        ActionType action
        Object data
    }

    class Response {
        boolean success
        String message
        Object data
    }

    class ScheduleLifecycleDTO {
        String requestEmployeeId
        String scheduleId
        String action  "PUBLISH | DISABLE"
    }

    class ScheduleMessages {
        +PUBLISH_SUCCESS
        +PUBLISH_FAILED_PREFIX
        +DISABLE_SUCCESS
        +DISABLE_FAILED_PREFIX
        +SCHEDULE_NOT_FOUND_BY_ID
        +UNAUTHORIZED
        +ONLY_DRAFT_CAN_BE_PUBLISHED
        +WRONG_STATUS_DISABLE
        +TICKETS_SOLD_BLOCKED
        +PRICE_NOT_CONFIGURED
    }

    class ActionType {
        <<enumeration>>
        PUBLISH_OR_DISABLE_SCHEDULE
        -- existing --
        CREATE_SCHEDULE
        UPDATE_SCHEDULE
        FILTER_SCHEDULE
    }

    class StatusSchedule {
        <<enumeration>>
        DRAFT
        NOT_STARTED
        IN_PROGRESS
        PAUSED
        READY
        COMPLETED
        CANCELLED
    }

    class Schedule {
        String id
        LocalDateTime departureTime
        LocalDateTime arrivalTime
        StatusSchedule status
        -- associations --
        Train train
        Route route
        List~ScheduleDetail~ scheduleDetails
    }

    class ScheduleDetail {
        String id
        BigDecimal priceSeat
        -- associations --
        Seat seat
        Schedule schedule
        RouteStop routeStop
        int version
    }

    class Seat {
        String id
        int number
        SeatType type
    }

    class Ticket {
        String id
        TicketType type
        boolean roundTrip
        TicketStatus status
        String qrCode
    }

    class Employee {
        String employeeId
        Boolean isManager
    }

    class RequestRouter {
        ScheduleService scheduleService
        Response route(Request)
    }

    class ScheduleServiceImpl {
        ScheduleRepository repository
        EmployeeRepository employeeRepository
        ScheduleDetailRepository scheduleDetailRepository
        Response publishSchedule(ScheduleLifecycleDTO)
        Response disableSchedule(ScheduleLifecycleDTO)
        -Employee findRequester(String employeeId)
    }

    class ScheduleRepositoryImpl {
        Schedule findScheduleById(EntityManager, String)
        boolean updateScheduleStatus(EntityManager, String, StatusSchedule)
        boolean deleteSchedule(EntityManager, String)
    }

    class ScheduleDetailRepositoryImpl {
        long countSoldSeatsByScheduleId(EntityManager, String)
        boolean existsUnpricedSeat(EntityManager, String)
    }

    Request *-- ActionType
    Request *-- ScheduleLifecycleDTO
    Response *-- ScheduleMessages

    ActionType ..> ScheduleServiceImpl : triggers
    ScheduleServiceImpl ..> ScheduleLifecycleDTO : receives
    ScheduleServiceImpl ..> ScheduleMessages : uses constants
    ScheduleServiceImpl *-- ScheduleRepositoryImpl
    ScheduleServiceImpl *-- ScheduleDetailRepositoryImpl
    ScheduleServiceImpl *-- EmployeeRepositoryImpl

    ScheduleRepositoryImpl ..> Schedule
    ScheduleDetailRepositoryImpl ..> ScheduleDetail
    ScheduleDetailRepositoryImpl ..> Ticket

    Schedule "1" o-- "many" ScheduleDetail
    ScheduleDetail "many" o-- "1" Seat
    Ticket "many" o-- "1" ScheduleDetail
```
