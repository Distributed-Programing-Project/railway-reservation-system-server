# Class Diagram & DB Mapping — Train Booking System

> Distributed Java system — Client/Server over TCP Socket  
> Updated: 2026-05-05

---

## 1. System Architecture

```mermaid
graph TD
    subgraph CLIENT ["☕ Client — JavaFX"]
        UI["JavaFX UI\n(Controllers + FXML)"]
        SC["SocketRequestService"]
        SM["SessionManager"]
    end

    subgraph COMMON ["📦 common (shared)"]
        REQ["Request { ActionType, Object }"]
        RES["Response { boolean, String, Object }"]
        AT["ActionType enum"]
        DTO["DTOs (Serializable)"]
        CONST["Constants / Enums"]
    end

    subgraph SERVER ["⚙️ Server — Java 21"]
        NET["Server.java\n(ServerSocket + ThreadPool)"]
        RR["RequestRouter\n(switch ActionType)"]
        SVC["Service Layer\n(business logic + validation)"]
        REPO["Repository Layer\n(JPA / JPQL)"]
        MAPPER["MapStruct Mappers\n(Entity ↔ DTO)"]
        MODEL["@Entity Models (15 entities)"]
    end

    subgraph DB ["🗄️ MariaDB :3307"]
        TABLES["manage_train\n(16 tables)"]
    end

    UI --> SC --> REQ
    RES --> SC --> UI
    REQ -- "ObjectOutputStream / TCP" --> NET
    NET --> RR --> SVC
    SVC --> MAPPER --> MODEL
    SVC --> REPO --> MODEL --> TABLES
    CLIENT -.->|depends on| COMMON
    SERVER -.->|depends on| COMMON
```

---

## 2. Class Diagram — Entities

```mermaid
classDiagram
    direction TB

    %% ── Auth ──────────────────────────────────────────────
    class Role {
        +String id
        +String code
        +String name
    }

    class Account {
        +String id
        +String username
        +String password
        +boolean active
    }

    class Employee {
        +String id
        +String employeeCode
        +String employeeName
        +String nationalId
        +String address
        +LocalDate dateOfBirth
        +Boolean gender
        +String phoneNumber
        +String email
        +Boolean isManager
        +EmployeeStatus status
        +LocalDate createdAt
        +LocalDate updatedAt
    }

    %% ── Khach hang ────────────────────────────────────────
    class Customer {
        +String id
        +String fullName
        +String idCard
        +String passport
        +String phoneNumber
        +String email
        +boolean isActive
        +int rewardPoints
    }

    %% ── Tau - Toa - Ghe ───────────────────────────────────
    class Train {
        +String id
        +String trainCode
        +TrainStatus status
    }

    class Carriage {
        +String id
        +int number
        +CarriageType type
    }

    class Seat {
        +String id
        +int number
        +boolean available
        +SeatType type
    }

    %% ── Ga - Tuyen - Diem dung ────────────────────────────
    class Station {
        +String id
        +String name
        +Float destinationKm
    }

    class Route {
        +String id
        +String routeCode
        +RouteStatus status
        +Double priceBasic
    }

    class RouteStop {
        +String id
        +int orderStop
    }

    %% ── Lich trinh ────────────────────────────────────────
    class Schedule {
        +String id
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +StatusSchedule status
    }

    class ScheduleDetail {
        +String id
        +BigDecimal priceSeat
        +Integer segmentDepartureOrder
        +Integer segmentDestinationOrder
        +int version
    }

    %% ── Ve - Hoa don ──────────────────────────────────────
    class Ticket {
        +String id
        +TicketType type
        +boolean roundTrip
        +TicketStatus status
        +String qrCode
        +String originalTicketId
        +boolean exchanged
        +String passengerName
        +String passengerIdCard
    }

    class Invoice {
        +String id
        +LocalDateTime issueDate
        +double totalAmount
        +InvoiceType type
        +String taxCode
        +String companyName
    }

    class InvoiceDetail {
        +String id
        +Double subTotal
        +double discount
        +double insurance
        +boolean isReturned
        +double refundAmount
    }

    %% ── Composition — parent sở hữu lifecycle của child (cascade) ──
    Train "1" *-- "*" Carriage
    Carriage "1" *-- "*" Seat
    Route "1" *-- "*" RouteStop
    Schedule "1" *-- "*" ScheduleDetail
    Invoice "1" *-- "*" InvoiceDetail

    %% ── Aggregation — whole/part, child tồn tại độc lập ──────────
    Employee "1" o-- "1" Account

    %% ── Association — FK reference ────────────────────────────────
    Account "*" --> "*" Role
    Route "*" --> "1" Station : departure
    Route "*" --> "1" Station : destination
    RouteStop "*" --> "1" Station
    Schedule "*" --> "1" Train
    Schedule "*" --> "1" Route
    ScheduleDetail "*" --> "1" Seat
    ScheduleDetail "*" --> "0..1" RouteStop
    ScheduleDetail "*" --> "1" Station : segmentDeparture
    ScheduleDetail "*" --> "1" Station : segmentDestination
    Ticket "*" --> "1" Customer
    Ticket "*" --> "1" ScheduleDetail
    Invoice "*" --> "1" Customer
    Invoice "*" --> "1" Employee
    InvoiceDetail "*" --> "1" Ticket
```

---

## 3. DB Mapping — ER Diagram

```mermaid
erDiagram
    accounts {
        VARCHAR36  account_id    PK
        VARCHAR50  username      "UNIQUE NOT NULL"
        VARCHAR255 password      "NOT NULL"
        BOOLEAN    is_active     "NOT NULL"
    }

    roles {
        VARCHAR36   role_id   PK
        VARCHAR30   role_code "UNIQUE NOT NULL"
        NVARCHAR100 role_name "NOT NULL"
    }

    account_roles {
        VARCHAR36 account_id FK
        VARCHAR36 role_id    FK
    }

    employees {
        VARCHAR12   employee_id       PK
        VARCHAR10   employee_code     "UNIQUE NOT NULL"
        NVARCHAR100 employee_name     "NOT NULL"
        VARCHAR12   national_id       "UNIQUE NOT NULL"
        NVARCHAR255 address
        DATE        date_of_birth
        BOOLEAN     gender
        VARCHAR10   phone_number
        VARCHAR100  email             "UNIQUE"
        BOOLEAN     is_manager        "NOT NULL"
        VARCHAR50   employment_status "NOT NULL"
        DATE        created_at        "NOT NULL"
        DATE        updated_at
        VARCHAR36   account_id        FK
    }

    customers {
        VARCHAR11   customer_id   PK
        NVARCHAR255 full_name     "NOT NULL"
        VARCHAR20   id_card
        VARCHAR20   passport
        VARCHAR10   phone_number
        VARCHAR100  email
        BOOLEAN     is_active     "NOT NULL"
        INT         reward_points
    }

    trains {
        VARCHAR6   train_id   PK
        VARCHAR10  train_code "UNIQUE NOT NULL"
        VARCHAR50  status
    }

    carriages {
        VARCHAR36 carriage_id     PK
        VARCHAR6  train_id        FK
        INT       sequence_number
        VARCHAR50 carriage_type
    }

    seats {
        VARCHAR36 seat_id         PK
        VARCHAR36 carriage_id     FK
        INT       sequence_number
        BOOLEAN   is_available
        VARCHAR50 seat_type
    }

    stations {
        VARCHAR6    station_id   PK
        NVARCHAR255 station_name
        FLOAT       destination_km
    }

    routes {
        VARCHAR8  route_id               PK
        VARCHAR6  departure_station_id   FK
        VARCHAR6  destination_station_id FK
        VARCHAR20 route_code
        VARCHAR50 status
        DOUBLE    price_basic
    }

    route_stops {
        VARCHAR36 route_stop_id   PK
        VARCHAR8  route_id        FK
        VARCHAR6  station_stop_id FK
        INT       order_stop
    }

    schedules {
        VARCHAR10 schedule_id    PK
        VARCHAR6  train_id       FK
        VARCHAR8  route_id       FK
        DATETIME  departure_time
        DATETIME  arrival_time
        VARCHAR50 status
    }

    schedule_details {
        VARCHAR36 schedule_detail_id             PK
        VARCHAR36 schedule_id                    FK
        VARCHAR36 seat_id                        FK
        VARCHAR36 route_stop_id                  FK "nullable"
        VARCHAR6  segment_departure_station_id   FK
        VARCHAR6  segment_destination_station_id FK
        DECIMAL   price_seat
        INT       segment_departure_order
        INT       segment_destination_order
        INT       version
    }

    tickets {
        VARCHAR10   ticket_id           PK
        VARCHAR11   customer_id         FK
        VARCHAR36   schedule_detail_id  FK
        VARCHAR50   ticket_type
        BOOLEAN     is_round_trip
        VARCHAR50   status
        TEXT        qr_code
        VARCHAR36   original_ticket_id
        BOOLEAN     is_exchanged
        NVARCHAR255 passenger_name
        VARCHAR20   passenger_id_card
    }

    invoices {
        VARCHAR10   invoice_id    PK
        VARCHAR11   customer_id   FK
        VARCHAR12   employee_id   FK
        DATETIME    issue_date
        DOUBLE      total_amount
        VARCHAR50   invoice_type
        VARCHAR50   tax_code
        NVARCHAR255 company_name
    }

    invoice_details {
        VARCHAR36 invoice_detail_id PK
        VARCHAR10 invoice_id        FK
        VARCHAR10 ticket_id         FK
        DOUBLE    sub_total
        DOUBLE    discount
        DOUBLE    insurance_fee
        BOOLEAN   is_returned
        DOUBLE    refund_amount
    }

    accounts        ||--o{ account_roles    : "account_id"
    roles           ||--o{ account_roles    : "role_id"
    accounts        ||--o| employees        : "account_id"
    employees       ||--o{ invoices         : "employee_id"
    customers       ||--o{ tickets          : "customer_id"
    customers       ||--o{ invoices         : "customer_id"
    invoices        ||--o{ invoice_details  : "invoice_id"
    tickets         ||--o{ invoice_details  : "ticket_id"
    schedule_details ||--o{ tickets         : "schedule_detail_id"
    schedules       ||--o{ schedule_details : "schedule_id"
    seats           ||--o{ schedule_details : "seat_id"
    route_stops     |o--o{ schedule_details : "route_stop_id (nullable)"
    stations        ||--o{ schedule_details : "segment_departure_station_id"
    stations        ||--o{ schedule_details : "segment_destination_station_id"
    trains          ||--o{ schedules        : "train_id"
    routes          ||--o{ schedules        : "route_id"
    trains          ||--o{ carriages        : "train_id"
    carriages       ||--o{ seats            : "carriage_id"
    stations        ||--o{ routes           : "departure_station_id"
    stations        ||--o{ routes           : "destination_station_id"
    routes          ||--o{ route_stops      : "route_id"
    stations        ||--o{ route_stops      : "station_stop_id"
```

---

## 4. Class Diagram — Core DTOs

```mermaid
classDiagram
    direction TB

    class AccountDTO {
        <<DTO>>
        +String id
        +String username
        +boolean active
        +String employeeId
        +boolean isManager
    }

    class EmployeeDTO {
        <<DTO>>
        +String employeeId
        +String employeeCode
        +String employeeName
        +String nationalId
        +String address
        +LocalDate dateOfBirth
        +Boolean gender
        +String phoneNumber
        +String email
        +Boolean isManager
        +EmployeeStatus employeeStatus
        +LocalDate createdAt
        +LocalDate updatedAt
    }

    class CustomerDTO {
        <<DTO>>
        +String customerId
        +String fullName
        +String idCard
        +String passport
        +String phone
        +String email
        +Boolean isActive
        +int rewardPoints
    }

    class TrainDTO {
        <<DTO>>
        +String id
        +String trainCode
        +TrainStatus status
    }

    class CarriageDTO {
        <<DTO>>
        +String id
        +int number
        +CarriageType type
        +String trainId
    }

    class SeatDTO {
        <<DTO>>
        +String id
        +int number
        +boolean available
        +SeatType type
        +String carriageId
    }

    class StationDTO {
        <<DTO>>
        +String id
        +String name
        +Float destinationKm
    }

    class RouteDTO {
        <<DTO>>
        +String id
        +String routeCode
        +String departureStationId
        +String destinationStationId
        +RouteStatus status
        +Double priceBasic
    }

    class RouteStopDTO {
        <<DTO>>
        +String id
        +int orderStop
        +String stationStopId
        +String routeId
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
    }

    class ScheduleDetailDTO {
        <<DTO>>
        +String id
        +double seatPrice
        +String scheduleId
        +String seatId
        +String routeStopId
        +String segmentDepartureStationId
        +String segmentDepartureStationName
        +String segmentDestinationStationId
        +String segmentDestinationStationName
        +Integer segmentDepartureOrder
        +Integer segmentDestinationOrder
        +int seatNumber
        +SeatType seatType
        +int carriageNumber
        +boolean sold
    }

    class TicketDTO {
        <<DTO>>
        +String id
        +String customerId
        +String scheduleDetailId
        +TicketType type
        +boolean roundTrip
        +TicketStatus status
        +String qrCode
        +String originalTicketId
        +boolean exchanged
        +String passengerName
        +String passengerIdCard
    }

    class InvoiceDTO {
        <<DTO>>
        +String id
        +LocalDateTime issueDate
        +double totalAmount
        +InvoiceType type
        +String customerId
        +String employeeId
        +String taxCode
        +String companyName
        +List~InvoiceDetailDTO~ details
    }

    class InvoiceDetailDTO {
        <<DTO>>
        +String id
        +String invoiceId
        +String ticketId
        +Double subTotal
        +double discount
        +double insurance
        +boolean isReturned
        +double refundAmount
    }
```

---

## 5. Enums

```mermaid
classDiagram
    direction LR

    class TrainStatus {
        <<enum>>
        ACTIVE
        MAINTENANCE
        INACTIVE
    }

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

    class RouteStatus {
        <<enum>>
        DRAFT
        ACTIVE
        PAUSED
        CANCELLED
    }

    class StatusSchedule {
        <<enum>>
        DRAFT
        NOT_STARTED
        IN_PROGRESS
        PAUSED
        READY
        COMPLETED
        CANCELLED
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
        EXCHANGED
        RETURNED
    }

    class InvoiceType {
        <<enum>>
        SALE
        REFUND
        EXCHANGE
    }

    class EmployeeStatus {
        <<enum>>
        ACTIVE
        PAUSE
        INACTIVE
    }

    class StatisticsPeriod {
        <<enum>>
        DAY
        WEEK
        MONTH
    }
```

---

## 6. Ký hiệu

| Ký hiệu | Loại | Ý nghĩa |
|---|---|---|
| `A "1" *-- "*" B` | **Composition** ◆── | A sở hữu B, B không tồn tại nếu không có A (cascade delete) |
| `A "1" o-- "1" B` | **Aggregation** ◇── | A chứa B, nhưng B có thể tồn tại độc lập |
| `A "*" --> "1" B` | **Association** ──▶ | A tham chiếu B qua FK, không quản lý lifecycle |
| `A "*" --> "*" B` | **Association** ──▶ | Many-to-Many (join table) |
| `"0..1"` | Cardinality | Quan hệ nullable (FK có thể null) |
| `"1" -- "1"` | One-to-One |
| `"*" --> "*"` | Many-to-Many (qua join table) |
| `..>` | Dependency / DTO input |
| `<<entity · table_name>>` | JPA entity ánh xạ đến bảng `table_name` |
| `[UUID]` | PK tự sinh UUID |
| `[custom, len=N]` | PK dùng custom generator, độ dài N |
