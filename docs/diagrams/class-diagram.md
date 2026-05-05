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

### 2a. Auth & Nhân viên

```mermaid
classDiagram
    direction LR

    class Role {
        <<entity · roles>>
        +String id [UUID]
        +String code [unique]
        +String name
    }

    class Account {
        <<entity · accounts>>
        +String id [UUID]
        +String username [unique]
        +String password
        +boolean active
        +Set~Role~ roles
    }

    class Employee {
        <<entity · employees>>
        +String employeeId [custom, len=12]
        +String employeeCode [unique]
        +String employeeName
        +String nationalId [unique]
        +String address
        +LocalDate dateOfBirth
        +Boolean gender
        +String phoneNumber
        +String email [unique]
        +Boolean isManager
        +EmployeeStatus employeeStatus
        +LocalDate createdAt
        +LocalDate updatedAt
        +Account account
    }

    Account "*" --> "*" Role        : account_roles (join table)
    Employee "1" --> "1" Account    : account_id FK
```

### 2b. Khách hàng

```mermaid
classDiagram
    class Customer {
        <<entity · customers>>
        +String id [custom, len=11]
        +String name [full_name]
        +String idCard
        +String passport
        +String phoneNumber
        +String email
        +boolean isActive
        +int rewardPoints
    }
```

### 2c. Tàu — Toa — Ghế

```mermaid
classDiagram
    direction LR

    class Train {
        <<entity · trains>>
        +String id [custom, len=6]
        +String trainCode [unique]
        +TrainStatus status
    }

    class Carriage {
        <<entity · carriages>>
        +String id [UUID]
        +int number [sequence_number]
        +CarriageType type [carriage_type]
        +Train train
    }

    class Seat {
        <<entity · seats>>
        +String id [UUID]
        +int number [sequence_number]
        +boolean available [is_available]
        +SeatType type [seat_type]
        +Carriage carriage
    }

    Train "1" *-- "*" Carriage  : train_id FK
    Carriage "1" *-- "*" Seat   : carriage_id FK
```

### 2d. Ga — Tuyến — Điểm dừng

```mermaid
classDiagram
    direction LR

    class Station {
        <<entity · stations>>
        +String id [custom, len=6]
        +String name [station_name, NVARCHAR]
        +Float destinationKm
    }

    class Route {
        <<entity · routes>>
        +String id [custom, len=8]
        +String routeCode
        +RouteStatus status
        +Double priceBasic
        +Station departureStation
        +Station destinationStation
    }

    class RouteStop {
        <<entity · route_stops>>
        +String id [UUID]
        +int orderStop
        +Station stationStop
        +Route route
    }

    Route "*" --> "1" Station   : departure_station_id FK
    Route "*" --> "1" Station   : destination_station_id FK
    Route "1" *-- "*" RouteStop : route_id FK
    RouteStop "*" --> "1" Station : station_stop_id FK
```

### 2e. Lịch trình — Chi tiết lịch trình

```mermaid
classDiagram
    direction LR

    class Schedule {
        <<entity · schedules>>
        +String id [custom, len=10]
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +StatusSchedule status
        +Train train
        +Route route
    }

    class ScheduleDetail {
        <<entity · schedule_details>>
        +String id [UUID]
        +BigDecimal priceSeat
        +Seat seat
        +Schedule schedule
        +RouteStop routeStop [nullable]
        +Station segmentDepartureStation
        +Station segmentDestinationStation
        +Integer segmentDepartureOrder
        +Integer segmentDestinationOrder
        +int version [@Version]
    }

    Schedule "*" --> "1" Train         : train_id FK
    Schedule "*" --> "1" Route         : route_id FK
    Schedule "1" *-- "*" ScheduleDetail : schedule_id FK, cascade REMOVE
    ScheduleDetail "*" --> "1" Seat         : seat_id FK
    ScheduleDetail "*" --> "0..1" RouteStop : route_stop_id FK (nullable)
    ScheduleDetail "*" --> "1" Station      : segment_departure_station_id FK
    ScheduleDetail "*" --> "1" Station      : segment_destination_station_id FK
```

### 2f. Vé — Hóa đơn

```mermaid
classDiagram
    direction LR

    class Ticket {
        <<entity · tickets>>
        +String id [custom, len=10]
        +TicketType type [ticket_type]
        +boolean roundTrip [is_round_trip]
        +TicketStatus status
        +String qrCode [TEXT]
        +String originalTicketId
        +boolean exchanged [is_exchanged]
        +String passengerName [NVARCHAR]
        +String passengerIdCard
        +Customer customer
        +ScheduleDetail scheduleDetail
    }

    class Invoice {
        <<entity · invoices>>
        +String id [custom, len=10]
        +LocalDateTime issueDate
        +double totalAmount
        +InvoiceType type [invoice_type]
        +String taxCode
        +String companyName [NVARCHAR]
        +Customer customer
        +Employee employee
    }

    class InvoiceDetail {
        <<entity · invoice_details>>
        +String id [UUID]
        +Double subTotal
        +double discount
        +double insurance [insurance_fee]
        +boolean isReturned
        +double refundAmount
        +Invoice invoice
        +Ticket ticket
    }

    Customer "1" --> "*" Ticket         : customer_id FK
    ScheduleDetail "1" --> "*" Ticket   : schedule_detail_id FK
    Customer "1" --> "*" Invoice        : customer_id FK
    Employee "1" --> "*" Invoice        : employee_id FK
    Invoice "1" *-- "*" InvoiceDetail   : invoice_id FK, cascade ALL
    Ticket "1" --> "*" InvoiceDetail    : ticket_id FK
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

| Ký hiệu | Ý nghĩa |
|---|---|
| `"1" *-- "*"` | Composition — One-to-Many (cascade) |
| `"1" --> "*"` | Association — One-to-Many (FK) |
| `"*" --> "1"` | Many-to-One (FK nằm ở bảng Many) |
| `"1" -- "1"` | One-to-One |
| `"*" --> "*"` | Many-to-Many (qua join table) |
| `..>` | Dependency / DTO input |
| `<<entity · table_name>>` | JPA entity ánh xạ đến bảng `table_name` |
| `[UUID]` | PK tự sinh UUID |
| `[custom, len=N]` | PK dùng custom generator, độ dài N |
