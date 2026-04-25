# Diagrams — UC003: Trả vé

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        UI["TicketReturnView<br/>(Trả vé UI)"]
        SC["SocketClient"]
    end

    subgraph TRANSPORT ["TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() trả về Response"]
    end

    subgraph SERVER ["Server (Java Socket Server)"]
        SRV["Server.java<br/>handleClient(Socket)"]
        RR["RequestRouter.route(Request)"]
        SVC1["TicketServiceImpl<br/>.searchTicketsForReturn(ReturnTicketSearchDTO)"]
        SVC2["TicketServiceImpl<br/>.previewReturnTickets(ReturnTicketPreviewRequestDTO)"]
        SVC3["TicketServiceImpl<br/>.confirmReturnTickets(ReturnTicketConfirmDTO)"]
        JPA["JPAUtils.getEntityManager()"]
        T_REPO["TicketRepositoryImpl<br/>.findTicketsByCustomerIdCardWithStatus(...)<br/>.findTicketsByIdsWithSchedule(...)"]
        E_REPO["EmployeeRepositoryImpl<br/>.findEmployeeById(...)"]
        I_REPO["InvoiceRepositoryImpl<br/>.createInvoice(em, invoice)"]
        ID_REPO["InvoiceDetailRepositoryImpl<br/>.createInvoiceDetail(...)<br/>.findInvoiceDetailsByTicketIdsAndInvoiceType(...)<br/>.updateInvoiceDetails(...)"]
    end

    subgraph DB ["MariaDB"]
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
    ID_REPO -- "INSERT InvoiceDetail(refund) x N" --> T_INVD
    ID_REPO -- "JPQL: find sale InvoiceDetail by ticketIds" --> T_INVD
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
    actor Clerk as NhanVienBanVe
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
        Clerk->>UI: Đăng nhập thành công
        Clerk->>UI: Chọn màn hình "Trả vé"
        Clerk->>UI: Nhập CCCD/Hộ chiếu, bấm "Tìm kiếm"
        UI->>UI: new ReturnTicketSearchDTO(idCard)
        UI->>Socket: sendRequest(new Request(SEARCH_TICKETS_FOR_RETURN, searchDTO))
        Socket->>Server: ObjectOutputStream.writeObject(request)
        Server->>Router: route(request)
        Router->>Service: searchTicketsForReturn(searchDTO)

        Service->>Service: ValidationUtils.validate(searchDTO)
        alt Lỗi validation
            Service-->>Router: Response.error(DATA_INVALID_PREFIX + errors)
        else Hợp lệ
            Service->>Service: JPAUtils.getEntityManager() trả về em
            Service->>TicketRepo: findTicketsByCustomerIdCardWithStatus(em, idCard, PAID)
            TicketRepo->>DB: JPQL SELECT Ticket JOIN FETCH customer/scheduleDetail/schedule WHERE (idCard OR passport) AND status=PAID
            DB-->>TicketRepo: List of Ticket
            TicketRepo-->>Service: tickets
            Service-->>Router: Response.success(FIND_SUCCESS, List of ReturnTicketTicketDTO)
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
        alt Lỗi validation
            Service-->>Router: Response.error(DATA_INVALID_PREFIX + errors)
        else Hợp lệ
            Service->>Service: computeReturn(em, ticketIds)
            Service->>TicketRepo: findTicketsByIdsWithSchedule(em, distinctIds)
            TicketRepo->>DB: JPQL SELECT Ticket JOIN FETCH customer/scheduleDetail/schedule WHERE id IN :ids
            DB-->>TicketRepo: List of Ticket
            TicketRepo-->>Service: tickets

            loop Mỗi ticket
                Service->>Service: validate status=PAID
                Service->>Service: validate schedule and departureTime not null
                Service->>Service: validate minutesToDeparture tối thiểu 4h
                Service->>Service: feeRate = (minutesToDeparture nhỏ hơn 24h thì 20%, ngược lại 10%)
                Service->>Service: fee = max(price*feeRate, 10000), clamp fee to price
                Service->>Service: refundAmount = price - fee
            end
            Service-->>Router: Response.success(PREVIEW_SUCCESS, ReturnTicketPreviewDTO(totalTicketPrice, refundFee, refundAmount))
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
    alt Lỗi validation
        Service-->>Router: Response.error(DATA_INVALID_PREFIX + errors)
    else Hợp lệ
        Service->>Service: JPAUtils.getEntityManager() trả về em
        Service->>Service: em.getTransaction().begin()

        Service->>Service: computeReturn(em, ticketIds)
        alt refundAmount mismatch (tolerance 1.0)
            Service->>Service: rollbackQuietly(tx)
            Service-->>Router: Response.error(REFUND_AMOUNT_MISMATCH)
        else refundAmount khớp
            Service->>EmpRepo: findEmployeeById(em, employeeId)
            EmpRepo->>DB: em.find(Employee, employeeId) thực hiện SELECT employees
            DB-->>EmpRepo: Employee or null
            EmpRepo-->>Service: employee

            alt employee == null
                Service->>Service: rollbackQuietly(tx)
                Service-->>Router: Response.error(EmployeeMessages.notFoundById)
            else employee tồn tại
                Service->>Service: check tất cả ticket thuộc cùng customer
                alt Customer mismatch
                    Service->>Service: rollbackQuietly(tx)
                    Service-->>Router: Response.error(CUSTOMER_MISMATCH)
                else Cùng customer
                    Service->>InvRepo: createInvoice(em, Invoice(type=REFUND,totalAmount=totalRefundAmount,employee))
                    InvRepo->>DB: em.persist(Invoice) thực hiện INSERT invoices
                    DB-->>InvRepo: refundInvoiceId
                    InvRepo-->>Service: refundInvoice

                    loop Mỗi ticket (refund detail)
                        Service->>InvDetRepo: createInvoiceDetail(em, InvoiceDetail(isReturned=true, refundAmount, subTotal=ticketPrice))
                        InvDetRepo->>DB: em.persist(InvoiceDetail) thực hiện INSERT invoice_details
                        DB-->>InvDetRepo: ok
                        InvDetRepo-->>Service: ok
                    end

                    Service->>InvDetRepo: findInvoiceDetailsByTicketIdsAndInvoiceType(em, ticketIds, SALE)
                    InvDetRepo->>DB: JPQL SELECT InvoiceDetail JOIN FETCH invoice,ticket WHERE ticketId IN :ids AND invoice.type=SALE
                    DB-->>InvDetRepo: List of InvoiceDetail (sale)
                    InvDetRepo-->>Service: saleDetails

                    loop Mỗi saleDetail
                        Service->>Service: saleDetail.returned=true, saleDetail.refundAmount=refundAmountByTicketId[ticketId]
                    end
                    Service->>InvDetRepo: updateInvoiceDetails(em, saleDetails)
                    InvDetRepo->>DB: em.merge(InvoiceDetail) x N thực hiện UPDATE invoice_details
                    DB-->>InvDetRepo: ok
                    InvDetRepo-->>Service: ok

                    loop Mỗi ticket
                        Service->>Service: ticket.status=RETURNED, ticket.qrCode="INVALID"
                    end
                    Service->>TicketRepo: updateTickets(em, tickets)
                    TicketRepo->>DB: em.merge(Ticket) x N thực hiện UPDATE tickets
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

    alt IllegalArgumentException (từ computeReturn)
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
        +String idCard
        +serialVersionUID : long
    }
    note for ReturnTicketSearchDTO "DTO"

    class ReturnTicketPreviewRequestDTO {
        +List~String~ ticketIds
        +serialVersionUID : long
    }
    note for ReturnTicketPreviewRequestDTO "DTO"

    class ReturnTicketPreviewDTO {
        +double totalTicketPrice
        +double refundFee
        +double refundAmount
        +serialVersionUID : long
    }
    note for ReturnTicketPreviewDTO "DTO"

    class ReturnTicketConfirmDTO {
        +List~String~ ticketIds
        +double refundAmount
        +String employeeId
        +serialVersionUID : long
    }
    note for ReturnTicketConfirmDTO "DTO"

    class ReturnTicketTicketDTO {
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
    note for ReturnTicketTicketDTO "DTO"

    class Ticket {
        +String id
        +TicketStatus status
        +String qrCode
        +Customer customer
        +ScheduleDetail scheduleDetail
    }
    note for Ticket "entity"

    class ScheduleDetail {
        +String id
        +BigDecimal priceSeat
        +Schedule schedule
    }
    note for ScheduleDetail "entity"

    class Schedule {
        +String id
        +LocalDateTime departureTime
    }
    note for Schedule "entity"

    class Invoice {
        +String id
        +LocalDateTime issueDate
        +double totalAmount
        +InvoiceType type
        +Customer customer
        +Employee employee
    }
    note for Invoice "entity"

    class InvoiceDetail {
        +String id
        +double subTotal
        +boolean isReturned
        +double refundAmount
        +Invoice invoice
        +Ticket ticket
    }
    note for InvoiceDetail "entity"

    class TicketStatus {
        PAID
        RETURNED
        CANCELLED
        EXCHANGED
    }
    note for TicketStatus "enum"

    class InvoiceType {
        SALE
        REFUND
        EXCHANGE
    }
    note for InvoiceType "enum"

    class Request {
        +ActionType action
        +Object data
        +serialVersionUID : long
    }
    note for Request "common"

    class Response {
        +boolean success
        +String message
        +Object data
        +serialVersionUID : long
        +success(message, data)$
        +error(message)$
    }
    note for Response "common"

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
