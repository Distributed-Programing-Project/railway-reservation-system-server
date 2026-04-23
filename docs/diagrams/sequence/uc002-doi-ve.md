# Diagrams — UC002: Đổi vé

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        UI["TicketExchangeView<br/>(Đổi vé UI)"]
        SC["SocketClient"]
    end

    subgraph TRANSPORT ["TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() trả về Response"]
    end

    subgraph SERVER ["Server (Java Socket Server)"]
        SRV["Server.java<br/>handleClient(Socket)"]
        RR["RequestRouter.route(Request)<br/>(ActionType.EXCHANGE_TICKET)"]
        SVC["TicketServiceImpl<br/>.exchangeTickets(ExchangeTicketRequestDTO)"]
        JPA["JPAUtils.getEntityManager()"]
        T_REPO["TicketRepositoryImpl<br/>.findTicketsForExchange(...)<br/>.updateTicket(...)<br/>.createTicket(...)"]
        SD_REPO["ScheduleDetailRepositoryImpl<br/>.findById(...)<br/>.getSoldSeatIds(...)<br/>.updateScheduleDetail(...)"]
        I_REPO["InvoiceRepositoryImpl<br/>.createInvoice(em, invoice)"]
        ID_REPO["InvoiceDetailRepositoryImpl<br/>.createInvoiceDetail(em, detail)"]
    end

    subgraph DB ["MariaDB"]
        T_TICKET["tickets"]
        T_SD["schedule_details"]
        T_SCH["schedules"]
        T_SEAT["seats"]
        T_TRAIN["trains"]
        T_CUS["customers"]
        T_INV["invoices"]
        T_INVD["invoice_details"]
    end

    UI -- "new Request(EXCHANGE_TICKET, ExchangeTicketRequestDTO)" --> SC
    SC --> OOS
    OOS -- "TCP Socket" --> SRV
    SRV --> RR
    RR --> SVC

    SVC --> JPA
    SVC --> T_REPO
    SVC --> SD_REPO
    SVC --> I_REPO
    SVC --> ID_REPO

    T_REPO -- "JPQL: Ticket JOIN FETCH ScheduleDetail/Schedule" --> T_TICKET
    SD_REPO -- "em.find(ScheduleDetail)" --> T_SD
    SD_REPO -- "JPQL: sold seat ids (Ticket.status NOT IN CANCELLED/EXCHANGED/RETURNED)" --> T_TICKET
    SD_REPO -- "OptimisticLock via version field" --> T_SD
    T_REPO -- "UPDATE Ticket.status=EXCHANGED, is_exchanged=true" --> T_TICKET
    T_REPO -- "INSERT new Ticket (status=PAID, original_ticket_id=...)" --> T_TICKET
    I_REPO -- "INSERT Invoice(type=EXCHANGE)" --> T_INV
    ID_REPO -- "INSERT InvoiceDetail x N" --> T_INVD

    T_TICKET --> T_CUS
    T_SD --> T_SCH
    T_SD --> T_SEAT
    T_SCH --> T_TRAIN
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Clerk as Nhân viên bán vé
    participant UI as TicketExchangeView
    participant Socket as SocketClient
    participant Server as Server.java
    participant Router as RequestRouter
    participant Service as TicketServiceImpl
    participant TicketRepo as TicketRepositoryImpl
    participant SDRepo as ScheduleDetailRepositoryImpl
    participant InvRepo as InvoiceRepositoryImpl
    participant InvDetRepo as InvoiceDetailRepositoryImpl
    participant DB as MariaDB

    Clerk->>UI: Đăng nhập thành công
    Clerk->>UI: Chọn màn hình "Đổi vé"
    Clerk->>UI: Chọn "Đổi vé", chọn vé cũ + ghế/chuyến mới
    UI->>UI: new ExchangeTicketRequestDTO(oldTicketIds, newScheduleDetailIds, cashReceived, taxCode, companyName)
    UI->>Socket: sendRequest(new Request(EXCHANGE_TICKET, requestDTO))
    Socket->>Server: ObjectOutputStream.writeObject(request)
    Server->>Router: route(request)
    Router->>Service: exchangeTickets(requestDTO)

    Service->>Service: ValidationUtils.validate(requestDTO)
    alt Lỗi validation (@NotEmpty/@Min)
        Service-->>Router: Response.error(DATA_INVALID_PREFIX + errors)
        Router-->>Server: Response
        Server-->>Socket: Response
        Socket-->>UI: Response(success=false)
    else oldTicketIds.size != newScheduleDetailIds.size
        Service-->>Router: Response.error(COUNT_MISMATCH)
        Router-->>Server: Response
        Server-->>Socket: Response
        Socket-->>UI: Response(success=false)
    else Hợp lệ
        Service->>Service: JPAUtils.getEntityManager() trả về em
        Service->>Service: em.getTransaction().begin()

        Service->>EmpRepo: findEmployeeById(requestDTO.employeeId, em)
        EmpRepo->>DB: SELECT employees WHERE id = :employeeId
        DB-->>EmpRepo: Employee? employee
        EmpRepo-->>Service: employee

        alt employee == null
            Service->>Service: rollbackQuietly(transaction)
            Service-->>Router: Response.error(NOT_FOUND_EMPLOYEE)
        else employee.getIsManager() != true
            Service->>Service: rollbackQuietly(transaction)
            Service-->>Router: Response.error(MANAGER_ONLY)
        end

        Service->>TicketRepo: findTicketsForExchange(oldTicketIds, em)
        TicketRepo->>DB: JPQL SELECT t FROM Ticket t JOIN FETCH t.scheduleDetail sd JOIN FETCH sd.schedule s JOIN FETCH t.customer c WHERE t.id IN :ids AND t.isExchanged=false AND c.isActive=true
        DB-->>TicketRepo: List(Ticket) oldTickets
        TicketRepo-->>Service: oldTickets

        alt oldTickets.size != oldTicketIds.size
            Service->>Service: rollbackQuietly(transaction)
            Service-->>Router: Response.error(SOME_TICKETS_INVALID)
        else Đủ tickets
            Service->>Service: validateBusinessRules(oldTickets)
            alt ticket.isExchanged == true OR ticket.originalTicketId != null
                Service->>Service: rollbackQuietly(transaction)
                Service-->>Router: Response.error(TICKET_ALREADY_EXCHANGED)
            else ticket.status != PAID
                Service->>Service: rollbackQuietly(transaction)
                Service-->>Router: Response.error(TICKET_NOT_PAID)
            else nhỏ hơn 24h trước giờ khởi hành
                Service->>Service: rollbackQuietly(transaction)
                Service-->>Router: Response.error(EXCHANGE_TIME_EXPIRED)
            else Pass business rules
                loop Mỗi oldTicket
                    Service->>Service: oldTicket.exchanged=true, oldTicket.status=EXCHANGED
                    Service->>TicketRepo: updateTicket(em, oldTicket)
                    TicketRepo->>DB: em.merge(Ticket) thực hiện UPDATE tickets
                    DB-->>TicketRepo: ok
                    TicketRepo-->>Service: ok
                end

                loop i = 0..n-1 (oldTicketId sang newScheduleDetailId)
                    Service->>SDRepo: findById(newSeatId, em)
                    SDRepo->>DB: em.find(ScheduleDetail, newSeatId) thực hiện SELECT schedule_details
                    DB-->>SDRepo: ScheduleDetail? newSeat
                    SDRepo-->>Service: newSeat

                    alt newSeat == null
                        Service->>Service: throw IllegalArgumentException(scheduleDetailNotFound)
                    else newSeat tồn tại
                        Service->>SDRepo: getSoldSeatIdsWithLock(em, scheduleId) [cache theo scheduleId, SELECT FOR UPDATE]
                        SDRepo->>DB: SELECT sd.seat.id FROM ScheduleDetail sd WHERE sd.schedule.id=:scheduleId [PESSIMISTIC_WRITE]
                        DB-->>SDRepo: Set(String) soldSeatIds
                        SDRepo-->>Service: soldSeatIds

                        alt soldSeatIds contains newSeat.seat.id
                            Service->>Service: rollbackQuietly(transaction)
                            Service-->>Router: Response.error(SEAT_NOT_AVAILABLE)
                        else Ghế còn trống
                            Service->>SDRepo: updateScheduleDetail(em, newSeat)
                            SDRepo->>DB: em.merge(ScheduleDetail versioned) thực hiện optimistic lock check
                            DB-->>SDRepo: ok
                            SDRepo-->>Service: ok

                            Service->>TicketRepo: createTicket(newTicket{status=PAID, originalTicketId=oldTicket.id}, em)
                            TicketRepo->>DB: em.persist(Ticket) thực hiện INSERT tickets
                            DB-->>TicketRepo: ok
                            TicketRepo-->>Service: ok
                        end
                    end
                end

                Service->>InvRepo: createInvoice(em, Invoice{type=EXCHANGE,totalAmount=finalAmount,customer,employee,taxCode,companyName})
                InvRepo->>DB: em.persist(Invoice) thực hiện INSERT invoices
                DB-->>InvRepo: invoiceId
                InvRepo-->>Service: invoice

                loop Mỗi newTicket
                    Service->>InvDetRepo: createInvoiceDetail(em, InvoiceDetail{subTotal=finalAmount/n,isReturned=false})
                    InvDetRepo->>DB: em.persist(InvoiceDetail) thực hiện INSERT invoice_details
                    DB-->>InvDetRepo: ok
                    InvDetRepo-->>Service: ok
                end

                Service->>Service: em.getTransaction().commit()
                Service-->>Router: Response.success(EXCHANGE_SUCCESS(finalAmount), null)
            end
        end

        Router-->>Server: Response
        Server-->>Socket: Response + out.reset()
        Socket-->>UI: Response
    end

    alt OptimisticLockException (xung dot version khi chiem ghe)
        Service->>Service: rollbackQuietly(transaction)
        Service-->>Router: Response.error(DATA_CONFLICT)
    else Exception khác
        Service->>Service: rollbackQuietly(transaction)
        Service-->>Router: Response.error(EXCHANGE_FAILED_PREFIX + e.message)
    end
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class ExchangeTicketRequestDTO {
        +List~String~ oldTicketIds
        +List~String~ newScheduleDetailIds
        +double cashReceived
        +String taxCode
        +String companyName
        +serialVersionUID : long
    }
    note for ExchangeTicketRequestDTO "DTO"

    class Ticket {
        +String id
        +TicketStatus status
        +boolean exchanged
        +String originalTicketId
        +Customer customer
        +ScheduleDetail scheduleDetail
        +TicketType type
        +boolean roundTrip
    }
    note for Ticket "entity"

    class ScheduleDetail {
        +String id
        +BigDecimal priceSeat
        +int version
        +Seat seat
        +Schedule schedule
    }
    note for ScheduleDetail "entity"

    class Schedule {
        +String id
        +LocalDateTime departureTime
        +Train train
    }
    note for Schedule "entity"

    class Invoice {
        +String id
        +LocalDateTime issueDate
        +double totalAmount
        +InvoiceType type
        +String taxCode
        +String companyName
        +Customer customer
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
        EXCHANGED
        RETURNED
        CANCELLED
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

    ExchangeTicketRequestDTO ..> Request : "data field"
    Request --> Response : "socket cycle"

    Ticket "N" --> "1" Customer : customer
    Ticket "1" --> "1" ScheduleDetail : scheduleDetail
    ScheduleDetail "N" --> "1" Schedule : schedule
    ScheduleDetail "N" --> "1" Seat : seat
    Schedule "N" --> "1" Train : train

    Invoice "N" --> "1" Customer : customer
    Invoice "1" --> "N" InvoiceDetail : details
    InvoiceDetail "N" --> "1" Ticket : ticket
    InvoiceDetail "N" --> "1" Invoice : invoice

    Ticket --> TicketStatus : status
    Invoice --> InvoiceType : type
```
