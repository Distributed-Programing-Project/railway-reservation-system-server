# Diagrams — UC003: Trả vé

---

## 1. System Architecture

```mermaid
graph TB
  subgraph CLIENT["Client (JavaFX)"]
    UI["ReturnTicketController"]
    CS["ReturnTicketClientService"]
    SRS["SocketRequestService.send(Request)"]
  end

  subgraph TRANSPORT["TCP Socket Transport"]
    OOS["ObjectOutputStream.writeObject(Request)"]
    OIS["ObjectInputStream.readObject() -> Response"]
  end

  subgraph SERVER["Server (Java Socket Server)"]
    SRV["Server.handleClient(Socket)"]
    RR["RequestRouter.route(Request)<br/>(ActionType.*RETURN*)"]
    SVC["TicketServiceImpl<br/>searchTicketsForReturn / previewReturnTickets / confirmReturnTickets / getRefundReceipt"]
    JPA["JPAUtils.getEntityManager()"]
    T_REPO["TicketRepositoryImpl.findTicketsByIdsWithSchedule(...) / updateTickets(...)"]
    INV_REPO["InvoiceRepositoryImpl.createInvoice(...)"]
    INVD_REPO["InvoiceDetailRepositoryImpl<br/>createInvoiceDetail(...)<br/>findInvoiceDetailsByTicketIdsAndInvoiceType(...)<br/>updateInvoiceDetails(...)"]
    E_REPO["EmployeeRepositoryImpl.findEmployeeById(...)"]
  end

  subgraph DB["MariaDB"]
    T_TICKET["tickets"]
    T_SCH["schedules"]
    T_SD["schedule_details"]
    T_INV["invoices"]
    T_INVD["invoice_details"]
    T_EMP["employees"]
    T_CUS["customers"]
  end

  UI --> CS --> SRS --> OOS
  OOS -- "TCP Socket (ObjectStream)" --> SRV
  SRV --> RR --> SVC --> JPA
  SVC --> T_REPO --> T_TICKET
  SVC --> INV_REPO --> T_INV
  SVC --> INVD_REPO --> T_INVD
  SVC --> E_REPO --> T_EMP
  T_TICKET --> T_SD --> T_SCH
  T_TICKET --> T_CUS
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
  actor Clerk as Nhân viên bán vé
  participant UI as ReturnTicketController
  participant ClientSvc as ReturnTicketClientService
  participant Socket as SocketRequestService
  participant Server as Server.handleClient
  participant Router as RequestRouter.route
  participant TicketSvc as TicketServiceImpl
  participant TicketRepo as TicketRepositoryImpl
  participant InvRepo as InvoiceRepositoryImpl
  participant InvDetRepo as InvoiceDetailRepositoryImpl
  participant DB as MariaDB

  Clerk->>UI: Nhập mã vé/QR/CCCD -> Tra cứu
  UI->>ClientSvc: searchTicketsForReturn(query)
  ClientSvc->>Socket: send(Request(SEARCH_TICKETS_FOR_RETURN, ReturnTicketSearchDTO))
  Socket->>Server: writeObject(Request)
  Server->>Router: route(Request)
  Router->>TicketSvc: searchTicketsForReturn(dto)
  TicketSvc->>TicketRepo: findTicketByIdOrQrWithSchedule(...) / findTicketsByCustomerIdCardWithStatus(...) / ...
  TicketRepo->>DB: JPQL SELECT Ticket JOIN FETCH schedule/seat/carriage/customer ...
  TicketSvc-->>Router: Response.success("Lấy thông tin vé thành công", List<ReturnTicketTicketDTO>)

  Clerk->>UI: Chọn danh sách vé -> Xem trước hoàn
  UI->>ClientSvc: previewReturnTickets(ticketIds)
  ClientSvc->>Socket: send(Request(PREVIEW_RETURN_TICKETS, ReturnTicketPreviewRequestDTO))
  Router->>TicketSvc: previewReturnTickets(dto)
  TicketSvc->>TicketRepo: findTicketsByIdsWithSchedule(ticketIds)
  TicketRepo->>DB: JPQL SELECT Ticket JOIN FETCH customer/scheduleDetail/schedule WHERE id IN :ids
  TicketSvc->>TicketSvc: doComputeReturn(...) (>=4h, fee min 10k, làm tròn 1k)
  alt Different customerId
    TicketSvc-->>Router: Response.error("Tất cả các vé phải thuộc cùng một khách hàng để thực hiện trả theo lô.")
  else OK
    TicketSvc-->>Router: Response.success("Tính toán số tiền hoàn trả thành công", ReturnTicketPreviewDTO)
  end

  Clerk->>UI: Nhập refundAmount + employeeId -> Xác nhận trả
  UI->>ClientSvc: confirmReturnTickets(ticketIds, refundAmount, employeeId)
  ClientSvc->>Socket: send(Request(CONFIRM_RETURN_TICKETS, ReturnTicketConfirmDTO))
  Router->>TicketSvc: confirmReturnTickets(dto)
  TicketSvc->>TicketSvc: AbstractGenericRepositoryImpl.transactional(em -> doConfirmReturnTickets)
  TicketSvc->>InvRepo: createInvoice(Invoice REFUND)
  TicketSvc->>InvDetRepo: createInvoiceDetail(refundDetail) x N
  TicketSvc->>InvDetRepo: findInvoiceDetailsByTicketIdsAndInvoiceType(..., SALE)
  TicketSvc->>InvDetRepo: updateInvoiceDetails(...)
  TicketSvc->>TicketRepo: updateTickets(status=RETURNED, qrCode=INVALID)
  TicketSvc-->>Router: Response.success("Trả vé thành công", refundInvoiceId)

  Clerk->>UI: In biên lai
  UI->>ClientSvc: getRefundReceipt(refundInvoiceId)
  ClientSvc->>Socket: send(Request(GET_REFUND_RECEIPT, RefundReceiptRequestDTO))
  Router->>TicketSvc: getRefundReceipt(dto)
  TicketSvc->>DB: JPQL SELECT Invoice i LEFT JOIN FETCH i.details d LEFT JOIN FETCH d.ticket ... WHERE i.id=:invoiceId
  TicketSvc-->>Router: Response.success("Lấy dữ liệu biên lai hoàn tiền thành công.", RefundReceiptDTO)
```

---

## 3. Class Diagram (DTO / Entity / Enum)

```mermaid
classDiagram
  direction TB

  class ReturnTicketSearchDTO["<<DTO>> ReturnTicketSearchDTO"] {
    String query
    ReturnTicketSearchType queryType
  }
  class ReturnTicketPreviewRequestDTO["<<DTO>> ReturnTicketPreviewRequestDTO"] {
    List~String~ ticketIds
  }
  class ReturnTicketConfirmDTO["<<DTO>> ReturnTicketConfirmDTO"] {
    List~String~ ticketIds
    double refundAmount
    String employeeId
  }
  class RefundReceiptRequestDTO["<<DTO>> RefundReceiptRequestDTO"] {
    String refundInvoiceId
  }
  class RefundReceiptDTO["<<DTO>> RefundReceiptDTO"] {
    String refundInvoiceId
    String ticketId
    double originalAmount
    double refundFee
    double refundAmount
  }

  class Ticket["<<entity>> Ticket"] {
    String id
    TicketStatus status
    String qrCode
  }
  class Invoice["<<entity>> Invoice"] {
    String id
    InvoiceType type
    double totalAmount
  }
  class InvoiceDetail["<<entity>> InvoiceDetail"] {
    String id
    boolean isReturned
    double refundAmount
  }
  class Employee["<<entity>> Employee"] {
    String employeeId
  }
  class Customer["<<entity>> Customer"] {
    String id
  }

  class TicketStatus["<<enum>> TicketStatus"]
  class InvoiceType["<<enum>> InvoiceType"]
  class ReturnTicketSearchType["<<enum>> ReturnTicketSearchType"]

  Ticket --> TicketStatus
  Invoice --> InvoiceType
  ReturnTicketSearchDTO --> ReturnTicketSearchType
  Customer "1" --o "N" Ticket
  Invoice "1" --o "N" InvoiceDetail
  InvoiceDetail "*" --> "1" Ticket
  Employee "1" --o "N" Invoice
```
