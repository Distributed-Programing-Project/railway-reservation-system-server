# Sequence Diagrams — UC002: Đổi vé

## 1) System Architecture

```mermaid
graph TB
  subgraph CLIENT["Client (JavaFX)"]
    UI["\"Exchange flow UI\""]
    SocketSvc["\"SocketRequestService\""]
  end

  subgraph TRANSPORT["TCP Socket Transport"]
    OOS["\"ObjectOutputStream.writeObject(Request)\""]
    OIS["\"ObjectInputStream.readObject() -> Response\""]
  end

  subgraph SERVER["Server (Java Socket Server)"]
    Handler["\"Server.handleClient(Socket)\""]
    Router["\"RequestRouter.route(Request)\""]
    TicketSvc["\"TicketServiceImpl\""]
    HoldStore["\"SeatHoldStore\""]
    Repos["\"Repositories (Ticket/Invoice/ScheduleDetail/Employee)\""]
  end

  subgraph DB["MariaDB"]
    Tickets["\"tickets\""]
    ScheduleDetails["\"schedule_details\""]
    Seats["\"seats\""]
    Schedules["\"schedules\""]
    Employees["\"employees\""]
    Invoices["\"invoices\""]
    InvoiceDetails["\"invoice_details\""]
  end

  UI --> SocketSvc --> OOS
  OOS -- "\"TCP ObjectStream\"" --> Handler --> Router --> TicketSvc
  TicketSvc --> HoldStore
  TicketSvc --> Repos --> DB
  Repos --> Tickets
  Repos --> ScheduleDetails
  Repos --> Seats
  Repos --> Schedules
  Repos --> Employees
  Repos --> Invoices
  Repos --> InvoiceDetails
  Handler -- "\"Response\"" --> OIS --> SocketSvc --> UI
```

---

## 2) Sequence Diagram

```mermaid
sequenceDiagram
  actor Clerk as "Nhân viên bán vé"
  participant UI as "Exchange UI"
  participant Socket as "SocketRequestService"
  participant Server as "Server.handleClient"
  participant Router as "RequestRouter.route"
  participant TicketSvc as "TicketServiceImpl"
  participant Hold as "SeatHoldStore"
  participant DB as "MariaDB"

  Clerk->>UI: "Nhập giấy tờ khách hàng"
  UI->>Socket: "send(Request{ActionType.SEARCH_TICKETS_FOR_EXCHANGE, data=ExchangeEligibleTicketSearchDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>TicketSvc: "searchTicketsForExchange(dto)"
  TicketSvc->>DB: "SELECT tickets (status=PAID) theo idCard"
  TicketSvc-->>Router: "Response.success(List<ExchangeEligibleTicketDTO>)"
  Router-->>Socket: "Response"
  Socket-->>UI: "Response"

  Clerk->>UI: "Chọn vé cũ + ghế mới (đã hold)"
  UI->>Socket: "send(Request{ActionType.PREVIEW_EXCHANGE_TICKETS, data=ExchangeTicketPreviewRequestDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>TicketSvc: "previewExchangeTickets(dto)"
  alt "COUNT_MISMATCH / INVALID_SESSION / SOME_TICKETS_INVALID"
    TicketSvc-->>Router: "Response.error(TicketMessages.*)"
  else "OK"
    TicketSvc->>DB: "SELECT old tickets + actual paid amount"
    loop "for each newScheduleDetailId"
      TicketSvc->>Hold: "isHeldBy(sdId, clientSessionId)"
    end
    TicketSvc->>DB: "SELECT new schedule_details + priceSeat"
    TicketSvc-->>Router: "Response.success(ExchangeTicketPreviewDTO)"
  end
  Router-->>Socket: "Response"
  Socket-->>UI: "Response"

  Clerk->>UI: "Xác nhận đổi vé"
  UI->>Socket: "send(Request{ActionType.EXCHANGE_TICKET, data=ExchangeTicketRequestDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>TicketSvc: "exchangeTickets(dto)"
  alt "Business rule / seat not available / employee not found"
    TicketSvc-->>Router: "Response.error(EmployeeMessages.* or TicketMessages.*)"
  else "OK (transactional)"
    TicketSvc->>DB: "UPDATE old tickets (EXCHANGED, qrCode=INVALID)"
    loop "for each new seat"
      TicketSvc->>Hold: "isHeldBy(sdId, clientSessionId)"
      TicketSvc->>DB: "LOCK + check sold seats (getSoldSeatIdsWithLock)"
      TicketSvc->>DB: "INSERT new ticket (PAID) + set qrCode=ticketId"
    end
    TicketSvc->>DB: "INSERT invoice (EXCHANGE) + invoice_details"
    TicketSvc-->>Router: "Response.success(ExchangeTicketResponseDTO)"
    TicketSvc->>Hold: "releaseAll(newScheduleDetailIds, clientSessionId)"
  end
  Router-->>Socket: "Response"
  Socket-->>UI: "Response"
```

