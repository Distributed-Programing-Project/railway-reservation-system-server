# Sequence Diagrams — UC001: Bán vé

## 1) System Architecture

```mermaid
graph TB
  subgraph CLIENT["Client (JavaFX)"]
    UI["SellTicketWizardController"]
    SocketSvc["SocketRequestService"]
  end

  subgraph TRANSPORT["TCP Socket Transport"]
    OOS["ObjectOutputStream.writeObject(Request)"]
    OIS["ObjectInputStream.readObject() -> Response"]
  end

  subgraph SERVER["Server (Java Socket Server)"]
    Handler["Server.handleClient(Socket)"]
    Router["RequestRouter.route(Request)"]
    SaleSvc["SaleServiceImpl"]
    PaySvc["PaymentOrderServiceImpl"]
    Store["InternalPaymentOrderStore"]
    JPA["JPAUtils / EntityManager"]
    Repos["Repositories (JPQL/JPA)"]
  end

  subgraph DB["MariaDB"]
    Stations["stations"]
    Schedules["schedules"]
    ScheduleDetails["schedule_details"]
    Tickets["tickets"]
    Customers["customers"]
    Invoices["invoices"]
    InvoiceDetails["invoice_details"]
    InvoiceMeta["invoice_metadata"]
  end

  UI --> SocketSvc --> OOS
  OOS -->|"TCP ObjectStream"| Handler
  Handler --> Router
  Router --> SaleSvc
  Router --> PaySvc
  SaleSvc --> JPA --> Repos --> DB
  PaySvc --> Store
  Repos --> Stations
  Repos --> Schedules
  Repos --> ScheduleDetails
  Repos --> Tickets
  Repos --> Customers
  Repos --> Invoices
  Repos --> InvoiceDetails
  Repos --> InvoiceMeta
  Handler -->|"Response"| OIS --> SocketSvc --> UI
```

---

## 2) Sequence Diagram (Client → Socket → Service → DB)

```mermaid
sequenceDiagram
  actor Clerk as "Nhân viên bán vé"
  participant UI as "SellTicketWizardController"
  participant Socket as "SocketRequestService"
  participant Server as "Server.handleClient"
  participant Router as "RequestRouter.route"
  participant SaleSvc as "SaleServiceImpl"
  participant PaySvc as "PaymentOrderServiceImpl"
  participant Store as "InternalPaymentOrderStore"
  participant DB as "MariaDB"

  Clerk->>UI: "openSaleScreen()"

  UI->>Socket: "send(Request{ActionType.FIND_ALL_STATIONS, data=null})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>SaleSvc: "findAllStations()"
  SaleSvc->>DB: "selectStations()"
  SaleSvc-->>Router: "Response.success(stations)"
  Router-->>Server: "Response"
  Server-->>Socket: "Response"
  Socket-->>UI: "Response"

  UI->>Socket: "send(Request{ActionType.SEARCH_SCHEDULES_FOR_SALE, data=SaleScheduleSearchDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>SaleSvc: "searchSchedulesForSale(SaleScheduleSearchDTO)"
  alt "Validate fail"
    SaleSvc-->>Router: "Response.error(SaleMessages.*)"
  else "OK"
    SaleSvc->>DB: "selectSchedulesForSale(criteria)"
    SaleSvc-->>Router: "Response.success(searchResult)"
  end
  Router-->>Server: "Response"
  Server-->>Socket: "Response"
  Socket-->>UI: "Response"

  UI->>Socket: "send(Request{ActionType.GET_SEATMAP_FOR_SCHEDULE, data=SeatMapRequestDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>SaleSvc: "getSeatMapForSchedule(SeatMapRequestDTO)"
  SaleSvc->>DB: "selectSeatMap(scheduleId)"
  SaleSvc->>DB: "selectSoldSeats(scheduleId)"
  SaleSvc-->>Router: "Response.success(seatMap)"
  Router-->>Server: "Response"
  Server-->>Socket: "Response"
  Socket-->>UI: "Response"

  UI->>Socket: "send(Request{ActionType.HOLD_SEATS_FOR_SALE, data=SeatHoldRequestDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>SaleSvc: "holdSeatsForSale(SeatHoldRequestDTO)"
  SaleSvc-->>Router: "Response.success(holdResult)"
  Router-->>Server: "Response"
  Server-->>Socket: "Response"
  Socket-->>UI: "Response"

  opt "Thanh toán online (PaymentMethod.ONLINE)"
    UI->>Socket: "send(Request{ActionType.CREATE_PAYMENT_ORDER, data=PaymentCreateRequestDTO})"
    Socket->>Server: "writeObject(Request)"
    Server->>Router: "route(Request)"
    Router->>PaySvc: "createPaymentOrder(PaymentCreateRequestDTO)"
    PaySvc->>Store: "createOrder(amount, sessionId)"
    PaySvc-->>Router: "Response.success(paymentOrder)"
    Router-->>Socket: "Response"
    Socket-->>UI: "Response"

    UI->>Socket: "send(Request{ActionType.CONFIRM_PAYMENT_ORDER, data=PaymentStatusRequestDTO})"
    Socket->>Server: "writeObject(Request)"
    Server->>Router: "route(Request)"
    Router->>PaySvc: "confirmPaymentOrder(PaymentStatusRequestDTO)"
    PaySvc->>Store: "confirmOrder(orderId, sessionId)"
    PaySvc-->>Router: "Response.success(paymentStatus)"
    Router-->>Socket: "Response"
    Socket-->>UI: "Response"
  end

  UI->>Socket: "send(Request{ActionType.CREATE_SALE_TRANSACTION, data=SaleCreateRequestDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>SaleSvc: "createSaleTransaction(SaleCreateRequestDTO)"
  alt "Business rule / payment fail"
    SaleSvc-->>Router: "Response.error(SaleMessages.*)"
  else "OK (transactional)"
    SaleSvc->>DB: "insertInvoiceAndMetadata()"
    SaleSvc->>DB: "insertTicketsAndInvoiceDetails()"
    SaleSvc->>DB: "updateCustomerRewardPoints()"
    opt "Nếu ONLINE"
      SaleSvc->>Store: "consumeOrder(orderId, invoiceId, sessionId)"
    end
    SaleSvc-->>Router: "Response.success(saleResult)"
  end
  Router-->>Server: "Response"
  Server-->>Socket: "Response"
  Socket-->>UI: "Response"
```
