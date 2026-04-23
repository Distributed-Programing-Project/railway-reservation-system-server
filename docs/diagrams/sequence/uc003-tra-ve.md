# Diagrams — UC003: Trả vé

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["🖥️ Client (JavaFX)"]
        UI["TicketReturnView\n(Trả vé UI)"]
        SC["SocketClient"]
    end

    subgraph TRANSPORT ["🔌 TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() → Response"]
    end

    subgraph SERVER ["⚙️ Server (Java Socket Server)"]
        SRV["Server.java\nhandleClient(Socket)"]
        RR["RequestRouter.route(Request)"]
        SVC1["TicketServiceImpl\n.searchTicketsForReturn(ReturnTicketSearchDTO)"]
        SVC2["TicketServiceImpl\n.previewReturnTickets(ReturnTicketPreviewRequestDTO)"]
        SVC3["TicketServiceImpl\n.confirmReturnTickets(ReturnTicketConfirmDTO)"]
        JPA["JPAUtils.getEntityManager()"]
        T_REPO["TicketRepositoryImpl\n.findTicketsByCustomerIdCardWithStatus(...)\n.findTicketsByIdsWithSchedule(...)"]
        E_REPO["EmployeeRepositoryImpl\n.findEmployeeById(...)"]
        I_REPO["InvoiceRepositoryImpl\n.createInvoice(em, invoice)"]
        ID_REPO["InvoiceDetailRepositoryImpl\n.createInvoiceDetail(...)\n.findInvoiceDetailsByTicketIdsAndInvoiceType(...)\n.updateInvoiceDetails(...)"]
    end

    subgraph DB ["🗄️ MariaDB"]
        T_TICKET["tickets"]
        T_CUS["customers"]
        T_SD["schedule_details"]
        T_SCH["schedules"]
        T_EMP["employees"]
        T_INV["invoices"]
        T_INVD["invoice_details"]
    end

    UI -- "Request(SEARCH_TICKETS_FOR_RETURN, ReturnTicketSearchDTO)" --> SC
    UI -- "Request(PREVIEW_RETURN_TICKETS, ReturnTicketPreviewRequestDTO)" --> SC
    UI -- "Request(CONFIRM_RETURN_TICKETS, ReturnTicketConfirmDTO)" --> SC

    SC --> OOS
    OOS -- "TCP Socket" --> SRV
    SRV --> RR
    RR --> SVC1
    RR --> SVC2
    RR --> SVC3

    SVC1 --> JPA
    SVC2 --> JPA
    SVC3 --> JPA

    SVC1 --> T_REPO
    SVC2 --> T_REPO
    SVC3 --> T_REPO
    SVC3 --> E_REPO
    SVC3 --> I_REPO
    SVC3 --> ID_REPO

    T_REPO -- "JPQL: Ticket JOIN FETCH customer/scheduleDetail/schedule" --> T_TICKET
    E_REPO -- "em.find(Employee)" --> T_EMP
    I_REPO -- "INSERT Invoice(type=REFUND)" --> T_INV
    ID_REPO -- "INSERT InvoiceDetail (refund) x N" --> T_INVD
    ID_REPO -- "JPQL: InvoiceDetail JOIN FETCH invoice,ticket WHERE invoice.type=SALE" --> T_INVD
    ID_REPO -- "UPDATE InvoiceDetail.returned/refund_amount" --> T_INVD
    T_REPO -- "UPDATE Ticket.status=RETURNED, qr_code=INVALID" --> T_TICKET

    T_TICKET --> T_CUS
    T_TICKET --> T_SD
    T_SD --> T_SCH
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Clerk as 👤 Nhân viên bán vé
    participant UI as TicketReturnView
    participant Socket as SocketClient
    participant Server as Server.java
    participant Router as RequestRouter
    participant Service as TicketServiceImpl
    participant TicketRepo as TicketRepositoryImpl
    participant EmpRepo as EmployeeRepositoryImpl
    participant InvRepo as InvoiceRepositoryImpl
    participant InvDetRepo as InvoiceDetailRepositoryImpl
    participant DB as MariaDB

    opt Tìm vé theo CCCD/Hộ chiếu (SEARCH_TICKETS_FOR_RETURN)
        Clerk->>UI: Nhập CCCD/Hộ chiếu, bấm "Tìm kiếm"
        UI->>UI: new ReturnTicketSearchDTO(idCard)
        UI->>Socket: sendRequest(new Request(SEARCH_TICKETS_FOR_RETURN, searchDTO))
        Socket->>Server: ObjectOutputStream.writeObject(request)
        Server->>Router: route(request)
        Router->>Service: searchTicketsForReturn(searchDTO)

        Service->>Service: ValidationUtils.validate(searchDTO)
        alt Lỗi validation (@NotEmpty)
            Service-->>Router: Response.error(DATA_INVALID_PREFIX + errors)
        else Hợp lệ
            Service->>Service: JPAUtils.getEntityManager() → em
            Service->>TicketRepo: findTicketsByCustomerIdCardWithStatus(em, idCard, PAID)
            TicketRepo->>DB: JPQL SELECT Ticket t JOIN FETCH t.customer c JOIN FETCH t.scheduleDetail sd JOIN FETCH sd.schedule s WHERE (c.idCard=:idCard OR c.passport=:idCard) AND t.status=:status
            DB-->>TicketRepo: List<Ticket>
            TicketRepo-->>Service: tickets
            Service-->>Router: Response.success(FIND_SUCCESS, List<ReturnTicketTicketDTO>)
        end

        Router-->>Server: Response
        Server-->>Socket: Response + out.reset()
        Socket-->>UI: Response
    end

    opt Xem trước phí/tiền hoàn (PREVIEW_RETURN_TICKETS)
        Clerk->>UI: Chọn ticketIds, bấm "Xem trước"
        UI->>UI: new ReturnTicketPreviewRequestDTO(ticketIds)
        UI->>Socket: sendRequest(new Request(PREVIEW_RETURN_TICKETS, previewDTO))
        Socket->>Server: ObjectOutputStream.writeObject(request)
        Server->>Router: route(request)
        Router->>Service: previewReturnTickets(previewDTO)

        Service->>Service: ValidationUtils.validate(previewDTO)
        alt Lỗi validation (@NotEmpty)
            Service-->>Router: Response.error(DATA_INVALID_PREFIX + errors)
        else Hợp lệ
            Service->>Service: computeReturn(em, ticketIds)
            Service->>Service: validate distinctIds + load tickets
            Service->>TicketRepo: findTicketsByIdsWithSchedule(em, distinctIds)
            TicketRepo->>DB: JPQL SELECT Ticket t JOIN FETCH t.customer c JOIN FETCH t.scheduleDetail sd JOIN FETCH sd.schedule s WHERE t.id IN :ids
            DB-->>TicketRepo: List<Ticket> tickets
            TicketRepo-->>Service: tickets

            loop Mỗi ticket
                Service->>Service: status == PAID ?
                Service->>Service: scheduleDetail/schedule/departureTime != null ?
                Service->>Service: minutesToDeparture >= 4h ?
                Service->>Service: feeRate = (minutesToDeparture < 24h ? 20% : 10%)
                Service->>Service: fee = max(price*feeRate, 10_000); fee<=price
                Service->>Service: refundAmount = price - fee
            end
            Service-->>Router: Response.success(PREVIEW_SUCCESS, ReturnTicketPreviewDTO{totalTicketPrice,refundFee,refundAmount})
        end

        Router-->>Server: Response
        Server-->>Socket: Response + out.reset()
        Socket-->>UI: Response
    end

    Clerk->>UI: Bấm "Xác nhận trả vé" (CONFIRM_RETURN_TICKETS)
    UI->>UI: new ReturnTicketConfirmDTO(ticketIds, refundAmount, employeeId)
    UI->>Socket: sendRequest(new Request(CONFIRM_RETURN_TICKETS, confirmDTO))
    Socket->>Server: ObjectOutputStream.writeObject(request)
    Server->>Router: route(request)
    Router->>Service: confirmReturnTickets(confirmDTO)

    Service->>Service: ValidationUtils.validate(confirmDTO)
    alt Lỗi validation (@NotEmpty/@Min)
        Service-->>Router: Response.error(DATA_INVALID_PREFIX + errors)
    else Hợp lệ
        Service->>Service: JPAUtils.getEntityManager() → em
        Service->>Service: em.getTransaction().begin()

        Service->>Service: computeReturn(em, ticketIds)
        alt |confirm.refundAmount - computed.totalRefundAmount| > 1.0
            note over Service,DB: Early return trong try; transaction vẫn active (code không rollback tường minh).
            Service-->>Router: Response.error(REFUND_AMOUNT_MISMATCH)
        else refundAmount khớp
            Service->>EmpRepo: findEmployeeById(em, employeeId)
            EmpRepo->>DB: em.find(Employee, employeeId) → SELECT employees
            DB-->>EmpRepo: Employee? employee
            EmpRepo-->>Service: employee

            alt employee == null
                note over Service,DB: Early return; không rollback tường minh.
                Service-->>Router: Response.error(EmployeeMessages.notFoundById)
            else employee tồn tại
                Service->>Service: check tất cả ticket thuộc cùng customer
                alt Customer mismatch
                    note over Service,DB: Early return; không rollback tường minh.
                    Service-->>Router: Response.error(CUSTOMER_MISMATCH)
                else Cùng customer
                    Service->>InvRepo: createInvoice(em, Invoice{type=REFUND,totalAmount=totalRefundAmount,employee})
                    InvRepo->>DB: em.persist(Invoice) → INSERT invoices
                    DB-->>InvRepo: refundInvoiceId
                    InvRepo-->>Service: refundInvoice

                    loop Mỗi ticket (refund detail)
                        Service->>InvDetRepo: createInvoiceDetail(em, InvoiceDetail{isReturned=true, refundAmount, subTotal=ticketPrice})
                        InvDetRepo->>DB: em.persist(InvoiceDetail) → INSERT invoice_details
                        DB-->>InvDetRepo: ok
                        InvDetRepo-->>Service: ok
                    end

                    Service->>InvDetRepo: findInvoiceDetailsByTicketIdsAndInvoiceType(em, ticketIds, SALE)
                    InvDetRepo->>DB: JPQL SELECT d FROM InvoiceDetail d JOIN FETCH d.invoice i JOIN FETCH d.ticket t WHERE t.id IN :ticketIds AND i.type=:invoiceType
                    DB-->>InvDetRepo: List<InvoiceDetail> saleDetails
                    InvDetRepo-->>Service: saleDetails

                    loop Mỗi saleDetail
                        Service->>Service: saleDetail.returned=true; saleDetail.refundAmount=refundAmountByTicketId[ticketId]
                    end
                    Service->>InvDetRepo: updateInvoiceDetails(em, saleDetails)
                    InvDetRepo->>DB: em.merge(InvoiceDetail) x N → UPDATE invoice_details
                    DB-->>InvDetRepo: ok
                    InvDetRepo-->>Service: ok

                    loop Mỗi ticket
                        Service->>Service: ticket.status=RETURNED; ticket.qrCode="INVALID"
                    end
                    Service->>TicketRepo: updateTickets(em, tickets)
                    TicketRepo->>DB: em.merge(Ticket) x N → UPDATE tickets
                    DB-->>TicketRepo: ok
                    TicketRepo-->>Service: ok

                    Service->>Service: em.getTransaction().commit()
                    Service-->>Router: Response.success(RETURN_SUCCESS, refundInvoiceId)
                end
            end
        end

        Router-->>Server: Response
        Server-->>Socket: Response + out.reset()
        Socket-->>UI: Response
    end

    alt IllegalArgumentException (từ computeReturn: ids trống/trùng/không hợp lệ/không đủ điều kiện thời gian)
        Service->>Service: rollbackQuietly(tx)
        Service-->>Router: Response.error(e.message)
    else OptimisticLockException
        Service->>Service: rollbackQuietly(tx)
        Service-->>Router: Response.error(DATA_CONFLICT)
    else Exception khác
        Service->>Service: rollbackQuietly(tx)
        Service-->>Router: Response.error(RETURN_FAILED_PREFIX + e.message)
    end
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class ReturnTicketSearchDTO {
        <<DTO>>
        +String idCard
        +serialVersionUID : long
    }

    class ReturnTicketPreviewRequestDTO {
        <<DTO>>
        +List~String~ ticketIds
        +serialVersionUID : long
    }

    class ReturnTicketPreviewDTO {
        <<DTO>>
        +double totalTicketPrice
        +double refundFee
        +double refundAmount
        +serialVersionUID : long
    }

    class ReturnTicketConfirmDTO {
        <<DTO>>
        +List~String~ ticketIds
        +double refundAmount
        +String employeeId
        +serialVersionUID : long
    }

    class ReturnTicketTicketDTO {
        <<DTO>>
        +String id
        +String customerId
        +String scheduleDetailId
        +String scheduleId
        +LocalDateTime departureTime
        +double ticketPrice
        +TicketType type
        +boolean roundTrip
        +TicketStatus status
        +String originalTicketId
        +serialVersionUID : long
    }

    class Ticket {
        <<entity>>
        +String id
        +TicketStatus status
        +String qrCode
        +Customer customer
        +ScheduleDetail scheduleDetail
    }

    class ScheduleDetail {
        <<entity>>
        +String id
        +BigDecimal priceSeat
        +Schedule schedule
    }

    class Schedule {
        <<entity>>
        +String id
        +LocalDateTime departureTime
    }

    class Invoice {
        <<entity>>
        +String id
        +LocalDateTime issueDate
        +double totalAmount
        +InvoiceType type
        +Customer customer
        +Employee employee
    }

    class InvoiceDetail {
        <<entity>>
        +String id
        +double subTotal
        +boolean isReturned
        +double refundAmount
        +Invoice invoice
        +Ticket ticket
    }

    class TicketStatus {
        <<enum>>
        PAID
        RETURNED
        CANCELLED
        EXCHANGED
    }

    class InvoiceType {
        <<enum>>
        SALE
        REFUND
        EXCHANGE
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

    ReturnTicketSearchDTO ..> Request : "SEARCH_TICKETS_FOR_RETURN"
    ReturnTicketPreviewRequestDTO ..> Request : "PREVIEW_RETURN_TICKETS"
    ReturnTicketConfirmDTO ..> Request : "CONFIRM_RETURN_TICKETS"
    Request --> Response : "socket cycle"

    Ticket "N" --> "1" Customer : customer
    Ticket "1" --> "1" ScheduleDetail : scheduleDetail
    ScheduleDetail "N" --> "1" Schedule : schedule

    Invoice "N" --> "1" Customer : customer
    Invoice "N" --> "1" Employee : employee
    Invoice "1" --> "N" InvoiceDetail : details
    InvoiceDetail "N" --> "1" Ticket : ticket
    InvoiceDetail "N" --> "1" Invoice : invoice

    Ticket --> TicketStatus : status
    Invoice --> InvoiceType : type
```
