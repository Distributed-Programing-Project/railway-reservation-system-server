# Diagrams — UC006: Tạo lịch trình tàu

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        UI["ScheduleManagementController"]
        SC["SocketClient"]
    end

    subgraph TRANSPORT ["TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() → Response"]
    end

    subgraph SERVER ["Server (Java Socket Server)"]
        SRV["Server.java\nhandleClient(Socket)"]
        RR["RequestRouter.route(Request)"]
        SVC["ScheduleServiceImpl\n.createSchedule(ScheduleCreateDTO)"]
        REPO["ScheduleRepositoryImpl\n.createScheduleWithDetails(em, schedule, trainId)"]
        JPA["JPAUtils.getEntityManager()"]
    end

    subgraph DB ["MariaDB"]
        T_SCH["schedules"]
        T_DET["schedule_details"]
        T_SEAT["seats / carriages"]
    end

    UI -- "new Request(CREATE_SCHEDULE, ScheduleCreateDTO)" --> SC
    SC --> OOS
    OOS -- "TCP Socket" --> SRV
    SRV -- "castData → ScheduleCreateDTO" --> RR
    RR --> SVC
    SVC --> JPA
    SVC --> REPO
    REPO -- "em.persist(Schedule)" --> T_SCH
    REPO -- "SELECT s.id FROM Seat WHERE train.id" --> T_SEAT
    REPO -- "em.persist(ScheduleDetail) x N" --> T_DET
    REPO -- "Response.success(scheduleId)" --> SVC
    SVC -- "Response" --> RR
    RR -- "Response" --> SRV
    SRV --> OIS
    OIS -- "TCP Socket" --> SC
    SC -- "response.isSuccess()" --> UI
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Manager as Nhân viên quản lý
    participant UI as ScheduleManagementController
    participant Socket as SocketClient
    participant Server as Server.java
    participant Router as RequestRouter
    participant Service as ScheduleServiceImpl
    participant Repo as ScheduleRepositoryImpl
    participant DB as MariaDB

    User->>UI: Đăng nhập thành công
    User->>UI: Chọn màn hình "Quản lý lịch trình"
    User->>UI: Nhấn "Tạo lịch trình"
    UI->>UI: Hiển thị form (trainId, routeId, departureTime, arrivalTime)
    Manager->>UI: Điền đầy đủ form, nhấn Xác nhận

    UI->>UI: new ScheduleCreateDTO(trainId, routeId, departureTime, arrivalTime)
    UI->>Socket: sendRequest(new Request(CREATE_SCHEDULE, scheduleCreateDTO))
    Socket->>Server: ObjectOutputStream.writeObject(request)

    Server->>Router: route(request)
    Router->>Router: castData(request, ScheduleCreateDTO.class)
    Router->>Service: createSchedule(scheduleCreateDTO)

    Service->>Service: ValidationUtils.validate(scheduleCreateDTO)
    alt Lỗi validation (trainId/routeId/departureTime null hoặc blank)
        Service-->>Router: Response.error("Mã tàu/tuyến/giờ không được để trống")
        Router-->>Server: Response
        Server-->>Socket: ObjectOutputStream.writeObject(response) + out.reset()
        Socket-->>UI: Response(success=false)
        UI-->>Manager: Hiển thị thông báo lỗi
    end

    Service->>Service: departureTime.isBefore(now().plusDays(1)) ?
    alt departureTime < now() + 1 ngày
        Service-->>Router: Response.error("Ngày khởi hành phải cách ít nhất 1 ngày")
        Router-->>Server: Response
        Server-->>Socket: ObjectOutputStream.writeObject(response) + out.reset()
        Socket-->>UI: Response(success=false)
        UI-->>Manager: Hiển thị thông báo lỗi
    end

    Service->>Service: arrivalTime != null && arrivalTime.isBefore(departureTime) ?
    alt arrivalTime < departureTime
        Service-->>Router: Response.error("Giờ đến không được nhỏ hơn giờ khởi hành")
        Router-->>Server: Response
        Server-->>Socket: ObjectOutputStream.writeObject(response) + out.reset()
        Socket-->>UI: Response(success=false)
        UI-->>Manager: Hiển thị thông báo lỗi
    end

    Service->>Service: JPAUtils.getEntityManager() → em
    Service->>Service: em.getTransaction().begin()
    Service->>Service: Build Schedule(train, route, departureTime, arrivalTime, DRAFT)

    Service->>Repo: createScheduleWithDetails(em, schedule, trainId)
    Repo->>Repo: schedule.setTrain(em.getReference(Train, trainId))
    Repo->>Repo: schedule.setRoute(em.getReference(Route, routeId))
    Repo->>DB: em.persist(schedule) → INSERT INTO schedules
    DB-->>Repo: schedule.id = UUID được sinh ra

    Repo->>DB: SELECT s.id FROM Seat s WHERE s.carriage.train.id = :trainId
    DB-->>Repo: List<String> seatIds (N ghế)

    loop Batch insert (50 records/flush)
        Repo->>Repo: ScheduleDetail.builder().schedule(ref).seat(ref).priceSeat(0).routeStop(null)
        Repo->>DB: em.persist(scheduleDetail) → INSERT INTO schedule_details
        Repo->>DB: em.flush() + em.clear() [mỗi 50 bản ghi]
    end

    Repo-->>Service: em.getReference(Schedule, scheduleId)

    Service->>DB: em.getTransaction().commit()
    Service->>Service: log.info("Schedule created: id={}, trainId={}")
    Service-->>Router: Response.success("Tạo lịch trình thành công", scheduleId)

    Router-->>Server: Response
    Server->>Socket: ObjectOutputStream.writeObject(response)
    Server->>Server: out.reset()
    Socket-->>UI: Response(success=true, data=scheduleId)
    UI-->>Manager: Đóng form, hiển thị "Tạo lịch trình thành công", làm mới danh sách
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class ScheduleCreateDTO {
        <<DTO>>
        +String trainId
        +String routeId
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +serialVersionUID : long
    }

    class ScheduleDTO {
        <<DTO>>
        +String id
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +StatusSchedule status
        +String trainId
        +String trainName
        +String routeId
        +String routeCode
        +String departureStationName
        +String destinationStationName
        +serialVersionUID : long
    }

    class Schedule {
        <<entity>>
        +String id
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +StatusSchedule status
        +Train train
        +Route route
    }

    class ScheduleDetail {
        <<entity>>
        +String id
        +BigDecimal priceSeat
        +Seat seat
        +Schedule schedule
        +RouteStop routeStop
    }

    class Train {
        <<entity>>
        +String id
        +String trainCode
        +TrainStatus status
        +List~Carriage~ carriages
    }

    class Route {
        <<entity>>
        +String id
        +String routeCode
        +Station departureStation
        +Station destinationStation
        +RouteStatus status
        +Double priceBasic
    }

    class StatusSchedule {
        <<enum>>
        DRAFT
        NOT_STARTED
        IN_PROGRESS
        PAUSED
        READY
        COMPLETED
    }

    class Request {
        <<common>>
        +ActionType action
        +Object data
        +serialVersionUID : long
    }

    class Response {
        <<common>>
        +boolean success
        +String message
        +Object data
        +serialVersionUID : long
        +success(message, data)$
        +error(message)$
    }

    ScheduleCreateDTO ..> Request : "data field"
    Request --> Response : "socket cycle"
    Schedule "N" --> "1" Train : train
    Schedule "N" --> "1" Route : route
    Schedule "1" --> "N" ScheduleDetail : (auto-generated)
    ScheduleDetail "N" --> "1" Schedule : schedule
    Schedule --> StatusSchedule : status
    ScheduleDTO ..> Schedule : "mapped by ScheduleMapper"
    ScheduleCreateDTO ..> Schedule : "creates"
```
