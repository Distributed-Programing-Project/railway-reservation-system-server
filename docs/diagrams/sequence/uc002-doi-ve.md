# Usecase 002: Đổi vé (UC002)

Reverse-engineered from `src/main/java/vn/edu/iuh/fit/server/service/impl/TicketServiceImpl.java` (`exchangeTickets`).

```mermaid
sequenceDiagram
    actor JavaFX_Client
    participant Socket_Router
    participant TicketService
    participant Repository
    participant Database

    JavaFX_Client->>Socket_Router: Gửi yêu cầu Đổi vé (ExchangeTicketRequestDTO)
    note over Socket_Router: Code hiện tại chưa expose action cho đổi vé trong ActionType/RequestRouter.\nSequence này mô tả đường gọi logic tới TicketService.exchangeTickets().
    Socket_Router->>TicketService: exchangeTickets(requestDTO)

    TicketService->>TicketService: ValidationUtils.validate(requestDTO)
    alt dữ liệu không hợp lệ (errors not empty)
        TicketService-->>Socket_Router: Response.error(DATA_INVALID + errors)
        Socket_Router-->>JavaFX_Client: Response.error
    else số lượng oldTicketIds != newScheduleDetailIds
        TicketService-->>Socket_Router: Response.error(COUNT_MISMATCH)
        Socket_Router-->>JavaFX_Client: Response.error
    else hợp lệ
        TicketService->>Database: EntityTransaction.begin()

        alt Happy path (không throw Exception)
            TicketService->>Repository: TicketRepository.findTicketsForExchange(oldTicketIds, em)
            Repository->>Database: JPQL SELECT Ticket JOIN FETCH ScheduleDetail/Schedule
            Database-->>Repository: List<Ticket> oldTickets
            Repository-->>TicketService: oldTickets

            alt oldTickets.size != requestDTO.oldTicketIds.size
                note over TicketService,Database: Early return trong try; code KHÔNG rollback transaction một cách tường minh.
                TicketService-->>Socket_Router: Response.error(SOME_TICKETS_INVALID)
            else đủ tickets
                TicketService->>TicketService: validateBusinessRules(oldTickets)
                alt ticket.isExchanged == true OR ticket.originalTicketId != null
                    note over TicketService,Database: Early return; KHÔNG rollback tường minh.
                    TicketService-->>Socket_Router: Response.error(TICKET_ALREADY_EXCHANGED)
                else ticket.status != PAID
                    note over TicketService,Database: Early return; KHÔNG rollback tường minh.
                    TicketService-->>Socket_Router: Response.error(TICKET_NOT_PAID)
                else hoursRemaining < 24h
                    note over TicketService,Database: Early return; KHÔNG rollback tường minh.
                    TicketService-->>Socket_Router: Response.error(EXCHANGE_TIME_EXPIRED)
                else pass business rules
                    loop for each oldTicket
                        TicketService->>Repository: TicketRepository.updateTicket(em, oldTicket{exchanged=true,status=EXCHANGED})
                        Repository->>Database: merge Ticket
                        Database-->>Repository: ok
                        Repository-->>TicketService: ok
                    end

                    loop for i = 0..n-1 (oldTicketId -> newScheduleDetailId)
                        TicketService->>Repository: ScheduleDetailRepository.findById(newSeatId, em)
                        Repository->>Database: em.find(ScheduleDetail, newSeatId)
                        Database-->>Repository: ScheduleDetail? newSeat
                        Repository-->>TicketService: newSeat

                        alt newSeat == null
                            TicketService->>TicketService: throw IllegalArgumentException(scheduleDetailNotFound)
                            TicketService->>Database: rollbackQuietly(transaction).rollback()
                            TicketService-->>Socket_Router: Response.error(EXCHANGE_FAILED_PREFIX + msg)
                        else newSeat tồn tại
                            opt cache soldSeatIds theo scheduleId (computeIfAbsent)
                                TicketService->>Repository: ScheduleDetailRepository.getSoldSeatIds(em, scheduleId)
                                Repository->>Database: JPQL SELECT sd.seat.id WHERE ticket.status NOT IN (CANCELLED,EXCHANGED,RETURNED)
                                Database-->>Repository: Set<String> seatIds
                                Repository-->>TicketService: soldSeatIds
                            end

                            alt soldSeatIds chứa newSeat.seat.id
                                note over TicketService,Database: Early return; KHÔNG rollback tường minh.
                                TicketService-->>Socket_Router: Response.error(SEAT_NOT_AVAILABLE)
                            else ghế còn trống
                                TicketService->>Repository: ScheduleDetailRepository.updateScheduleDetail(em, newSeat)
                                Repository->>Database: merge ScheduleDetail (có thể phát sinh OptimisticLockException)
                                Database-->>Repository: ok
                                Repository-->>TicketService: ok

                                TicketService->>Repository: TicketRepository.createTicket(newTicket{status=PAID,originalTicketId=oldTicket.id}, em)
                                Repository->>Database: persist Ticket
                                Database-->>Repository: ok
                                Repository-->>TicketService: ok
                            end
                        end
                    end

                    TicketService->>Repository: InvoiceRepository.createInvoice(em, Invoice{type=EXCHANGE,totalAmount=finalAmount})
                    Repository->>Database: persist Invoice
                    Database-->>Repository: invoiceId
                    Repository-->>TicketService: invoice

                    loop for each newTicket
                        TicketService->>Repository: InvoiceDetailRepository.createInvoiceDetail(em, InvoiceDetail{subTotal=finalAmount/n,isReturned=false})
                        Repository->>Database: persist InvoiceDetail
                        Database-->>Repository: ok
                        Repository-->>TicketService: ok
                    end

                    TicketService->>Database: EntityTransaction.commit()
                    TicketService-->>Socket_Router: Response.success(EXCHANGE_SUCCESS, null)
                end
            end

        else OptimisticLockException (xung đột dữ liệu)
            TicketService->>Database: rollbackQuietly(transaction).rollback()
            TicketService-->>Socket_Router: Response.error(DATA_CONFLICT)

        else Exception khác
            TicketService->>Database: rollbackQuietly(transaction).rollback()
            TicketService-->>Socket_Router: Response.error(EXCHANGE_FAILED_PREFIX + e.message)
        end

        Socket_Router-->>JavaFX_Client: Response
    end
```

