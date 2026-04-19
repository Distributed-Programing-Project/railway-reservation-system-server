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
        id : String «UUID»
        username : String «unique»
        password : String
        active : boolean
    }

    class Employee {
        <<entity>>
        employeeId : String «UUID»
        employeeName : String
        nationalId : String «CCCD»
        dateOfBirth : LocalDate
        gender : Boolean
        phoneNumber : String
        email : String
        isManager : Boolean
        employeeStatus : EmployeeStatus
        createdAt : LocalDate
        updatedAt : LocalDate
    }

    class Customer {
        <<entity>>
        id : String «UUID»
        name : String
        idCard : String
        passport : String
        phoneNumber : String
        email : String
    }

    class Train {
        <<entity>>
        id : String «UUID»
        status : TrainStatus
    }

    class Carriage {
        <<entity>>
        id : String «UUID»
        number : int
        type : CarriageType
    }

    class Seat {
        <<entity>>
        id : String «UUID»
        number : int
        type : SeatType
    }

    class Station {
        <<entity>>
        id : String «UUID»
        name : String
        destinationKm : Float
    }

    class Route {
        <<entity>>
        id : String «UUID»
        routeCode : String
        status : RouteStatus
        priceBasic : Double
    }

    class RouteStop {
        <<entity>>
        id : String «UUID»
        orderStop : int
    }

    class Schedule {
        <<entity>>
        id : String «UUID»
        departureTime : LocalDateTime
        arrivalTime : LocalDateTime
        status : StatusSchedule
    }

    class ScheduleDetail {
        <<entity>>
        id : String «UUID»
        priceSeat : BigDecimal
    }

    class Ticket {
        <<entity>>
        id : String «UUID»
        type : TicketType
        roundTrip : boolean
        status : TicketStatus
        qrCode : String
    }

    class Invoice {
        <<entity>>
        id : String «UUID»
        issueDate : LocalDateTime
        totalAmount : double
        type : InvoiceType
    }

    class InvoiceDetail {
        <<entity>>
        id : String «UUID»
        subTotal : double
        discount : double
        insurance : double
        isReturned : boolean
        refundAmount : double
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
        id : String
        username : String
        active : boolean
    }

    class TrainDTO {
        <<DTO / Serializable>>
        id : String
        status : TrainStatus
    }

    class CarriageDTO {
        <<DTO / Serializable>>
        id : String
        number : int
        type : CarriageType
        trainId : String
    }

    class SeatDTO {
        <<DTO / Serializable>>
        id : String
        number : int
        type : SeatType
        carriageId : String
    }

    class StationDTO {
        <<DTO / Serializable>>
        id : String
        name : String
        destinationKm : Float
    }

    class RouteDTO {
        <<DTO / Serializable>>
        id : String
        routeCode : String
        departureStationId : String
        destinationStationId : String
        status : RouteStatus
        priceBasic : Double
    }

    class RouteStopDTO {
        <<DTO / Serializable>>
        id : String
        orderStop : int
        stationStopId : String
        routeId : String
    }

    class ScheduleDTO {
        <<DTO / Serializable>>
        id : String
        departureTime : LocalDateTime
        arrivalTime : LocalDateTime
        status : StatusSchedule
        trainId : String
        routeId : String
    }

    class ScheduleDetailDTO {
        <<DTO / Serializable>>
        id : String
        seatPrice : double
        scheduleId : String
        seatId : String
        routeStopId : String
    }

    class CustomerDTO {
        <<DTO / Serializable>>
        id : String
        name : String
        idCard : String
        passport : String
        phoneNumber : String
        email : String
    }

    class TicketDTO {
        <<DTO / Serializable>>
        id : String
        customerId : String
        scheduleDetailId : String
        type : TicketType
        roundTrip : boolean
        status : TicketStatus
        qrCode : String
    }

    class EmployeeDTO {
        <<DTO / Serializable>>
        employeeId : String
        employeeName : String
        nationalId : String
        dateOfBirth : LocalDate
        gender : Boolean
        phoneNumber : String
        email : String
        isManager : Boolean
        employmentStatus : String
        createdAt : LocalDate
        updatedAt : LocalDate
    }

    class InvoiceDTO {
        <<DTO / Serializable>>
        id : String
        issueDate : LocalDateTime
        totalAmount : double
        type : InvoiceType
        customerId : String
        employeeId : String
    }

    class InvoiceDetailDTO {
        <<DTO / Serializable>>
        id : String
        invoiceId : String
        ticketId : String
        subTotal : double
        discount : double
        insurance : double
        isReturned : boolean
        refundAmount : double
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
