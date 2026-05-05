# Sequence Diagrams — UC003: Trả vé (Hoàn vé)

## 1) System Architecture

```mermaid
graph TB
  subgraph CLIENT["Client (JavaFX)"]
    UI["\"Return flow UI\""]
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
    Repos["\"Repositories (Ticket/Invoice/InvoiceDetail/Employee)\""]
  end

  subgraph DB["MariaDB"]
    Tickets["\"tickets\""]
    Invoices["\"invoices\""]
    InvoiceDetails["\"invoice_details\""]
    Employees["\"employees\""]
    ScheduleDetails["\"schedule_details\""]
    Schedules["\"schedules\""]
    Routes["\"routes\""]
    Stations["\"stations\""]
  end

  UI --> SocketSvc --> OOS
  OOS -- "\"TCP ObjectStream\"" --> Handler --> Router --> TicketSvc
  TicketSvc --> Repos --> DB
  Repos --> Tickets
  Repos --> Invoices
  Repos --> InvoiceDetails
  Repos --> Employees
  Repos --> ScheduleDetails
  Repos --> Schedules
  Repos --> Routes
  Repos --> Stations
  Handler -- "\"Response\"" --> OIS --> SocketSvc --> UI
```

---

## 2) Sequence Diagram

```mermaid
sequenceDiagram
  actor Clerk as "Nhân viên bán vé"
  participant UI as "Return UI"
  participant Socket as "SocketRequestService"
  participant Server as "Server.handleClient"
  participant Router as "RequestRouter.route"
  participant TicketSvc as "TicketServiceImpl"
  participant DB as "MariaDB"

  Clerk->>UI: "Nhập mã vé/QR/giấy tờ để tra cứu"
  UI->>Socket: "send(Request{ActionType.SEARCH_TICKETS_FOR_RETURN, data=ReturnTicketSearchDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>TicketSvc: "searchTicketsForReturn(dto)"
  TicketSvc->>DB: "SELECT tickets theo queryType (AUTO/TICKET_ID/BUYER_DOCUMENT/PASSENGER_DOCUMENT)"
  TicketSvc-->>Router: "Response.success(List<ReturnTicketTicketDTO>)"
  Router-->>Socket: "Response"
  Socket-->>UI: "Response"

  Clerk->>UI: "Chọn danh sách vé -> xem trước hoàn"
  UI->>Socket: "send(Request{ActionType.PREVIEW_RETURN_TICKETS, data=ReturnTicketPreviewRequestDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>TicketSvc: "previewReturnTickets(dto)"
  alt "ticketIds invalid / duplicate / not eligible"
    TicketSvc-->>Router: "Response.error(TicketMessages.*)"
  else "OK"
    TicketSvc->>DB: "SELECT tickets + schedule departureTime + actual paid amount"
    TicketSvc-->>Router: "Response.success(ReturnTicketPreviewDTO)"
  end
  Router-->>Socket: "Response"
  Socket-->>UI: "Response"

  Clerk->>UI: "Nhập refundAmount + employeeId -> xác nhận"
  UI->>Socket: "send(Request{ActionType.CONFIRM_RETURN_TICKETS, data=ReturnTicketConfirmDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>TicketSvc: "confirmReturnTickets(dto)"
  alt "refundAmount mismatch / employee not found / conflict"
    TicketSvc-->>Router: "Response.error(EmployeeMessages.* or TicketMessages.*)"
  else "OK (transactional)"
    TicketSvc->>DB: "INSERT invoice (REFUND)"
    TicketSvc->>DB: "INSERT invoice_details (insurance=0, isReturned=true)"
    TicketSvc->>DB: "UPDATE invoice_details of SALE (isReturned=true, refundAmount=...)"
    TicketSvc->>DB: "UPDATE tickets (RETURNED, qrCode=INVALID)"
    TicketSvc-->>Router: "Response.success(refundInvoiceId)"
  end
  Router-->>Socket: "Response"
  Socket-->>UI: "Response"

  Clerk->>UI: "In/tra cứu biên lai hoàn tiền"
  UI->>Socket: "send(Request{ActionType.GET_REFUND_RECEIPT, data=RefundReceiptRequestDTO})"
  Socket->>Server: "writeObject(Request)"
  Server->>Router: "route(Request)"
  Router->>TicketSvc: "getRefundReceipt(dto)"
  TicketSvc->>DB: "SELECT invoice + join details/ticket/schedule/route/stations"
  TicketSvc-->>Router: "Response.success(RefundReceiptDTO)"
  Router-->>Socket: "Response"
  Socket-->>UI: "Response"
```

