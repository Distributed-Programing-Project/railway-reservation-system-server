# Diagrams — UC001: Bán vé

---

## 1. System Architecture

```mermaid
graph TB
  subgraph CLIENT["Client (JavaFX)"]
    UI["SellTicketWizardController / Step*Controller<br/>(Bán vé UI)"]
    CS["SaleClientService"]
    SRS["SocketRequestService.send(Request)"]
  end

  subgraph TRANSPORT["TCP Socket Transport"]
    OOS["ObjectOutputStream.writeObject(Request)"]
    OIS["ObjectInputStream.readObject() -> Response"]
  end

  subgraph SERVER["Server (Java Socket Server)"]
    SRV["Server.handleClient(Socket)"]
    RR["RequestRouter.route(Request)"]
    SVC["SaleServiceImpl<br/>searchSchedulesForSale / getSeatMapForSchedule / holdSeatsForSale / createSaleTransaction"]
    JPA["JPAUtils.getEntityManager()"]
    SCH_REPO["ScheduleRepositoryImpl.filterSchedules(...)"]
    SD_REPO["ScheduleDetailRepositoryImpl.getSoldSeatIds(...)"]
    T_REPO["TicketRepositoryImpl.updateTickets(...)"]
    INV_REPO["InvoiceRepositoryImpl.createInvoice(...)"]
    INVD_REPO["InvoiceDetailRepositoryImpl.createInvoiceDetail(...)"]
    HOLD["SeatHoldStore (in-memory TTL=10m)"]
  end

  subgraph DB["MariaDB"]
    T_ST["stations"]
    T_R["routes"]
    T_SCH["schedules"]
    T_SD["schedule_details"]
    T_SEAT["seats"]
    T_CAR["carriages"]
    T_TRAIN["trains"]
    T_CUS["customers"]
    T_TICKET["tickets"]
    T_INV["invoices"]
    T_INVD["invoice_details"]
  end

  UI --> CS --> SRS --> OOS
  OOS -- "TCP Socket (ObjectStream)" --> SRV
  SRV --> RR --> SVC
  SRV <-- OIS
  SVC --> JPA
  SVC --> SCH_REPO --> T_SCH
  SCH_REPO --> T_R
  SCH_REPO --> T_TRAIN
  SVC --> SD_REPO --> T_TICKET
  SVC --> HOLD
  SVC --> T_REPO --> T_TICKET
  SVC --> INV_REPO --> T_INV
  SVC --> INVD_REPO --> T_INVD
  T_SD --> T_SEAT --> T_CAR --> T_TRAIN
  T_INV --> T_CUS
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
  actor Clerk as Nhân viên bán vé
  participant UI as SellTicketWizardController
  participant ClientSvc as SaleClientService
  participant Socket as SocketRequestService
  participant Server as Server.handleClient
  participant Router as RequestRouter.route
  participant SaleSvc as SaleServiceImpl
  participant Repo as JPA/JPQL (Repositories)
  participant DB as MariaDB

  Clerk->>UI: Chọn chức năng "Bán vé"
  UI->>ClientSvc: searchSchedulesForSale(SaleScheduleSearchDTO)
  ClientSvc->>Socket: send(new Request(SEARCH_SCHEDULES_FOR_SALE, dto))
  Socket->>Server: ObjectOutputStream.writeObject(Request)
  Server->>Router: route(Request)
  Router->>SaleSvc: searchSchedulesForSale(dto)
  alt Validate fail (stations/dates/request)
    SaleSvc-->>Router: Response.error("Dữ liệu yêu cầu không hợp lệ"/...)
  else OK
    SaleSvc->>Repo: ScheduleRepositoryImpl.filterSchedules(...)
    Repo->>DB: JPQL SELECT DISTINCT s FROM Schedule ... JOIN FETCH route/train ...
    DB-->>Repo: List<Schedule>
    Repo-->>SaleSvc: schedules
    SaleSvc-->>Router: Response.success("Tìm chuyến tàu thành công", SaleScheduleSearchResultDTO)
  end
  Router-->>Server: Response
  Server-->>Socket: Response
  Socket-->>UI: Response

  Clerk->>UI: Chọn chuyến -> xem sơ đồ ghế
  UI->>ClientSvc: getSeatMap(scheduleId, clientSessionId)
  ClientSvc->>Socket: send(new Request(GET_SEATMAP_FOR_SCHEDULE, SeatMapRequestDTO))
  Socket->>Server: writeObject(Request)
  Server->>Router: route(Request)
  Router->>SaleSvc: getSeatMapForSchedule(dto)
  SaleSvc->>Repo: JPQL SELECT sd FROM ScheduleDetail sd JOIN FETCH seat/carriage ...
  Repo->>DB: SELECT schedule_details + seats + carriages
  DB-->>Repo: List<ScheduleDetail>
  SaleSvc->>Repo: ScheduleDetailRepositoryImpl.getSoldSeatIds(scheduleId)
  Repo->>DB: JPQL SELECT sd.seat.id FROM Ticket t JOIN t.scheduleDetail sd WHERE ... status NOT IN (...)
  DB-->>Repo: Set<seatId>
  SaleSvc-->>Router: Response.success("Lấy sơ đồ chỗ ngồi thành công", SeatMapResponseDTO)

  Clerk->>UI: Chọn ghế -> Giữ ghế
  UI->>ClientSvc: holdSeats(scheduleId, scheduleDetailIds, clientSessionId)
  ClientSvc->>Socket: send(new Request(HOLD_SEATS_FOR_SALE, SeatHoldRequestDTO))
  Router->>SaleSvc: holdSeatsForSale(dto)
  SaleSvc->>Repo: Validate scheduleDetailIds thuộc schedule
  SaleSvc->>SaleSvc: SeatHoldStore.tryHold(...) (TTL=10 phút)
  SaleSvc-->>Router: Response.success("Giữ ghế thành công", SeatHoldResponseDTO)

  Clerk->>UI: Nhập hành khách/người mua -> Thanh toán
  UI->>ClientSvc: createSaleTransaction(SaleCreateRequestDTO)
  ClientSvc->>Socket: send(new Request(CREATE_SALE_TRANSACTION, dto))
  Router->>SaleSvc: createSaleTransaction(dto)
  SaleSvc->>SaleSvc: validate hold + ràng buộc (<=10 vé/chiều, khứ hồi, trẻ em, đổi điểm)
  alt Payment not ready / online invalid
    SaleSvc-->>Router: Response.error("Chưa đủ điều kiện thanh toán"/"Cần tạo đơn thanh toán online"/...)
  else OK
    SaleSvc->>Repo: AbstractGenericRepositoryImpl.transactional(...)
    Repo->>DB: em.persist(Invoice SALE)
    Repo->>DB: em.persist(Ticket PAID) x N
    Repo->>DB: em.persist(InvoiceDetail) x N
    SaleSvc-->>Router: Response.success("Bán vé thành công", SaleCreateResponseDTO)
  end
```

---

## 3. Class Diagram (DTO / Entity / Enum)

```mermaid
classDiagram
  direction TB

  class ActionType["<<enum>> ActionType"] {
    SEARCH_SCHEDULES_FOR_SALE
    GET_SEATMAP_FOR_SCHEDULE
    HOLD_SEATS_FOR_SALE
    RELEASE_HELD_SEATS_FOR_SALE
    CREATE_SALE_TRANSACTION
  }

  class Request["<<DTO>> Request"] {
    ActionType action
    Object data
  }
  class Response["<<DTO>> Response"] {
    boolean success
    String message
    Object data
  }

  class SaleScheduleSearchDTO["<<DTO>> SaleScheduleSearchDTO"] {
    String departureStationId
    String destinationStationId
    LocalDate departureDate
    TicketCategory ticketCategory
    LocalDate returnDate
    int page
    int size
  }

  class SeatMapRequestDTO["<<DTO>> SeatMapRequestDTO"] {
    String scheduleId
    String clientSessionId
  }

  class SeatHoldRequestDTO["<<DTO>> SeatHoldRequestDTO"] {
    String scheduleId
    List~String~ scheduleDetailIds
    String clientSessionId
  }

  class SaleCreateRequestDTO["<<DTO>> SaleCreateRequestDTO"] {
    String clientSessionId
    TicketCategory ticketCategory
    String outboundScheduleId
    String returnScheduleId
    List~String~ outboundScheduleDetailIds
    List~String~ returnScheduleDetailIds
    List~SalePassengerDTO~ outboundPassengers
    List~SalePassengerDTO~ returnPassengers
    List~SaleChildUnder6DTO~ childrenUnder6
    SaleBuyerDTO buyer
    SaleVatDTO vat
    SaleRedeemPointsDTO redeemPoints
    PaymentMethod paymentMethod
    Double amountPaid
    String paymentOrderId
  }

  class Ticket["<<entity>> Ticket"] {
    String id
    TicketStatus status
    boolean exchanged
    String originalTicketId
    String qrCode
  }
  class Invoice["<<entity>> Invoice"] {
    String id
    InvoiceType type
    double totalAmount
  }
  class InvoiceDetail["<<entity>> InvoiceDetail"] {
    String id
    Double subTotal
    boolean isReturned
    double refundAmount
  }
  class Customer["<<entity>> Customer"] {
    String id
    int rewardPoints
  }
  class ScheduleDetail["<<entity>> ScheduleDetail"] {
    String id
    BigDecimal priceSeat
    int version
  }
  class Schedule["<<entity>> Schedule"] {
    String id
    LocalDateTime departureTime
    LocalDateTime arrivalTime
  }

  class TicketStatus["<<enum>> TicketStatus"]
  class TicketCategory["<<enum>> TicketCategory"]
  class InvoiceType["<<enum>> InvoiceType"]
  class PaymentMethod["<<enum>> PaymentMethod"]

  Request --> ActionType
  Ticket --> TicketStatus
  Invoice --> InvoiceType
  SaleScheduleSearchDTO --> TicketCategory
  SaleCreateRequestDTO --> PaymentMethod

  Customer "1" --o "N" Ticket
  ScheduleDetail "1" -- "1" Ticket
  Schedule "1" --o "N" ScheduleDetail
  Invoice "1" --o "N" InvoiceDetail
  InvoiceDetail "*" --> "1" Ticket
```
