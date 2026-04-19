# Class Diagram — Train Booking System (Toàn bộ hệ thống)

> Distributed Java system — Client/Server over TCP Socket
> Generated: 2026-04-19

## System Architecture

```mermaid
graph TD
    subgraph CLIENT ["☕ Client — JavaFX (TCP Socket)"]
        UI["🖥️ JavaFX UI"]
        SC["SocketClient"]
    end

    subgraph COMMON ["📦 common (Maven Artifact)"]
        REQ["Request\n{ ActionType, Object }"]
        RES["Response\n{ boolean, String, Object }"]
        AT["ActionType (enum)"]
        DTO["DTOs (Serializable)"]
    end

    subgraph SERVER ["⚙️ Server — Java 21"]
        NET["NetworkHandler\n(ServerSocket)"]
        SVC["Service Layer"]
        REPO["Repository Layer\n(JPA/JPQL)"]
        MODEL["@Entity Models\n(14 entities)"]
    end

    subgraph DB ["🗄️ MariaDB :3307"]
        TABLES["manage_train\n(14 tables)"]
    end

    UI --> SC
    SC -- "ObjectOutputStream\nTCP Socket" --> NET
    NET -- "ObjectInputStream" --> SC
    SC --> UI
    NET --> SVC --> REPO --> MODEL --> TABLES
    CLIENT -.->|depends on| COMMON
    SERVER -.->|depends on| COMMON
```

## Class Diagram — Entities & Relationships

```mermaid
classDiagram
    direction LR

    class Account {
        <<entity>>
        -String id «UUID»
        -String username «unique»
        -String password
        -boolean active
    }

    class Employee {
        <<entity>>
        -String employeeId «UUID»
        -String employeeName
        -String nationalId «CCCD»
        -LocalDate dateOfBirth
        -Boolean gender
        -String phoneNumber
        -String email
        -Boolean isManager
        -EmployeeStatus employeeStatus
        -LocalDate createdAt
        -LocalDate updatedAt
    }

    class Customer {
        <<entity>>
        -String id «UUID»
        -String name
        -String idCard
        -String passport
        -String phoneNumber
        -String email
    }

    class Train {
        <<entity>>
        -String id «UUID»
        -TrainStatus status
    }

    class Carriage {
        <<entity>>
        -String id «UUID»
        -int number
        -CarriageType type
    }

    class Seat {
        <<entity>>
        -String id «UUID»
        -int number
        -SeatType type
    }

    class Station {
        <<entity>>
        -String id «UUID»
        -String name
        -Float destinationKm
    }

    class Route {
        <<entity>>
        -String id «UUID»
        -String routeCode
        -RouteStatus status
        -Double priceBasic
    }

    class RouteStop {
        <<entity>>
        -String id «UUID»
        -int orderStop
    }

    class Schedule {
        <<entity>>
        -String id «UUID»
        -LocalDateTime departureTime
        -LocalDateTime arrivalTime
        -StatusSchedule status
    }

    class ScheduleDetail {
        <<entity>>
        -String id «UUID»
        -BigDecimal priceSeat
    }

    class Ticket {
        <<entity>>
        -String id «UUID»
        -TicketType type
        -boolean roundTrip
        -TicketStatus status
        -String qrCode
    }

    class Invoice {
        <<entity>>
        -String id «UUID»
        -LocalDateTime issueDate
        -double totalAmount
        -InvoiceType type
    }

    class InvoiceDetail {
        <<entity>>
        -String id «UUID»
        -double subTotal
        -double discount
        -double insurance
        -boolean isReturned
        -double refundAmount
    }

    %% ══════════════════════════════════════
    %% RELATIONSHIPS — Tàu & Toa & Ghế
    %% ══════════════════════════════════════

    Train "1" --o "N" Carriage : chứa nhiều toa >
    Carriage "1" --o "N" Seat : chứa nhiều ghế >

    %% ══════════════════════════════════════
    %% RELATIONSHIPS — Ga & Tuyến & Điểm dừng
    %% ══════════════════════════════════════

    Station "1" --o "N" Route : ga đi (departure) >
    Station "1" --o "N" Route : ga đến (destination) >
    Station "1" --o "N" RouteStop : điểm dừng >
    Route "1" --o "N" RouteStop : các điểm dừng >

    %% ══════════════════════════════════════
    %% RELATIONSHIPS — Lịch trình
    %% ══════════════════════════════════════

    Route "1" --o "N" Schedule : có nhiều lịch >
    Train "1" --o "N" Schedule : chạy nhiều lịch >

    %% ══════════════════════════════════════
    %% RELATIONSHIPS — Chi tiết lịch trình
    %% ══════════════════════════════════════

    Schedule "1" --o "N" ScheduleDetail : chi tiết ghế >
    Seat "1" --o "N" ScheduleDetail : được đặt trong >
    RouteStop "1" --o "N" ScheduleDetail : tại điểm dừng >

    %% ══════════════════════════════════════
    %% RELATIONSHIPS — Vé
    %% ══════════════════════════════════════

    Customer "1" --o "N" Ticket : mua nhiều vé >
    ScheduleDetail "1" -- "1" Ticket : 1 ghế = 1 vé

    %% ══════════════════════════════════════
    %% RELATIONSHIPS — Hóa đơn
    %% ══════════════════════════════════════

    Customer "1" --o "N" Invoice : có nhiều hóa đơn >
    Employee "1" --o "N" Invoice : lập nhiều hóa đơn >
    Invoice "1" --o "N" InvoiceDetail : gồm nhiều dòng >
    Ticket "1" -- "1" InvoiceDetail : 1 vé = 1 dòng «unique»

    %% ══════════════════════════════════════
    %% RELATIONSHIPS — Tài khoản
    %% ══════════════════════════════════════

    Employee "1" -- "1" Account : đăng nhập bằng
```

## Class Diagram — DTOs

```mermaid
classDiagram
    direction TB

    class AccountDTO {
        <<DTO / Serializable>>
        +String id
        +String username
        +boolean active
    }

    class TrainDTO {
        <<DTO / Serializable>>
        +String id
        +TrainStatus status
    }

    class CarriageDTO {
        <<DTO / Serializable>>
        +String id
        +int number
        +CarriageType type
        +String trainId
    }

    class SeatDTO {
        <<DTO / Serializable>>
        +String id
        +int number
        +SeatType type
        +String carriageId
    }

    class StationDTO {
        <<DTO / Serializable>>
        +String id
        +String name
        +Float destinationKm
    }

    class RouteDTO {
        <<DTO / Serializable>>
        +String id
        +String routeCode
        +String departureStationId
        +String destinationStationId
        +RouteStatus status
        +Double priceBasic
    }

    class RouteStopDTO {
        <<DTO / Serializable>>
        +String id
        +int orderStop
        +String stationStopId
        +String routeId
    }

    class ScheduleDTO {
        <<DTO / Serializable>>
        +String id
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +StatusSchedule status
        +String trainId
        +String routeId
    }

    class ScheduleDetailDTO {
        <<DTO / Serializable>>
        +String id
        +double seatPrice
        +String scheduleId
        +String seatId
        +String routeStopId
    }

    class CustomerDTO {
        <<DTO / Serializable>>
        +String id
        +String name
        +String idCard
        +String passport
        +String phoneNumber
        +String email
    }

    class TicketDTO {
        <<DTO / Serializable>>
        +String id
        +String customerId
        +String scheduleDetailId
        +TicketType type
        +boolean roundTrip
        +TicketStatus status
        +String qrCode
    }

    class EmployeeDTO {
        <<DTO / Serializable>>
        +String employeeId
        +String employeeName
        +String nationalId
        +LocalDate dateOfBirth
        +Boolean gender
        +String phoneNumber
        +String email
        +Boolean isManager
        +String employmentStatus
        +LocalDate createdAt
        +LocalDate updatedAt
    }

    class InvoiceDTO {
        <<DTO / Serializable>>
        +String id
        +LocalDateTime issueDate
        +double totalAmount
        +InvoiceType type
        +String customerId
        +String employeeId
    }

    class InvoiceDetailDTO {
        <<DTO / Serializable>>
        +String id
        +String invoiceId
        +String ticketId
        +double subTotal
        +double discount
        +double insurance
        +boolean isReturned
        +double refundAmount
    }

    %% Entity ↔ DTO mapping
    AccountDTO ..|> Account : maps to
    TrainDTO ..|> Train : maps to
    CarriageDTO ..|> Carriage : maps to
    SeatDTO ..|> Seat : maps to
    StationDTO ..|> Station : maps to
    RouteDTO ..|> Route : maps to
    RouteStopDTO ..|> RouteStop : maps to
    ScheduleDTO ..|> Schedule : maps to
    ScheduleDetailDTO ..|> ScheduleDetail : maps to
    CustomerDTO ..|> Customer : maps to
    TicketDTO ..|> Ticket : maps to
    EmployeeDTO ..|> Employee : maps to
    InvoiceDTO ..|> Invoice : maps to
    InvoiceDetailDTO ..|> InvoiceDetail : maps to
```

## Enums

```mermaid
classDiagram
    direction LR

    class CarriageType {
        <<enum>>
        HARD_SEAT
        SOFT_SEAT
        SOFT_SEAT_AC
        BERTH_6
        BERTH_4
    }

    class SeatType {
        <<enum>>
        HARD_SEAT
        SOFT_SEAT
        VIP_SEAT
        BERTH_6
        BERTH_4
    }

    class TrainStatus {
        <<enum>>
        ACTIVE
        MAINTENANCE
        INACTIVE
    }

    class RouteStatus {
        <<enum>>
        DRAFT
        ACTIVE
        PAUSED
        CANCELLED
    }

    class StationStatus {
        <<enum>>
        DRAFT
        PAUSED
        READY
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

    class TicketType {
        <<enum>>
        NORMAL
        SENIOR
        CHILD
        STUDENT
    }

    class TicketStatus {
        <<enum>>
        BOOKED
        PAID
        CANCELLED
        USED
        EXPIRED
    }

    class EmployeeStatus {
        <<enum>>
        ACTIVE
        PAUSE
        INACTIVE
    }

    class InvoiceType {
        <<enum>>
        SALE
        REFUND
        EXCHANGE
    }

    %% Used by
    CarriageType <-- Carriage : uses
    SeatType <-- Seat : uses
    TrainStatus <-- Train : uses
    RouteStatus <-- Route : uses
    StatusSchedule <-- Schedule : uses
    TicketType <-- Ticket : uses
    TicketStatus <-- Ticket : uses
    EmployeeStatus <-- Employee : uses
    InvoiceType <-- Invoice : uses
```

## Ký hiệu Relationship

| Ký hiệu | Ý nghĩa | Ví dụ |
|----------|---------|-------|
| `"1" --o "N"` | One-to-Many (composition) | Train `1` --o `N` Carriage |
| `"1" -- "1"` | One-to-One | Employee `1` -- `1` Account |
| `..\|>` | DTO maps to Entity | TrainDTO ..\|> Train |
| `<--` | Enum used by Entity | TrainStatus <-- Train |
