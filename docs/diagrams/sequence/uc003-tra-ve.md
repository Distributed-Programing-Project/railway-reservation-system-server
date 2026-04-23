# Usecase 003: Trả vé (UC003)

Reverse-engineered from `src/main/java/vn/edu/iuh/fit/server/service/impl/TicketServiceImpl.java` (`previewReturnTickets`, `confirmReturnTickets`, `computeReturn`).

```mermaid
sequenceDiagram
    actor JavaFX_Client
    participant Socket_Router
    participant TicketService
    participant Repository
    participant Database

    opt (Tuỳ UI) Tìm vé theo CCCD/Hộ chiếu trước khi trả
        JavaFX_Client->>Socket_Router: Request(SEARCH_TICKETS_FOR_RETURN, ReturnTicketSearchDTO{idCard})
        Socket_Router->>TicketService: searchTicketsForReturn(searchDTO)
        TicketService->>TicketService: ValidationUtils.validate(searchDTO)
        alt dữ liệu không hợp lệ
            TicketService-->>Socket_Router: Response.error(DATA_INVALID + errors)
        else hợp lệ
            TicketService->>Repository: TicketRepository.findTicketsByCustomerIdCardWithStatus(em, idCard, PAID)
            Repository->>Database: JPQL SELECT Ticket JOIN FETCH customer/scheduleDetail/schedule WHERE idCard/passport AND status=PAID
            Database-->>Repository: List<Ticket>
            Repository-->>TicketService: tickets
            TicketService-->>Socket_Router: Response.success(FIND_SUCCESS, List<ReturnTicketTicketDTO>)
        end
        Socket_Router-->>JavaFX_Client: Response
    end

    opt (Tuỳ UI) Xem trước phí/tiền hoàn (Preview)
        JavaFX_Client->>Socket_Router: Request(PREVIEW_RETURN_TICKETS, ReturnTicketPreviewRequestDTO{ticketIds})
        Socket_Router->>TicketService: previewReturnTickets(previewRequestDTO)
        TicketService->>TicketService: ValidationUtils.validate(previewRequestDTO)
        alt dữ liệu không hợp lệ
            TicketService-->>Socket_Router: Response.error(DATA_INVALID + errors)
        else hợp lệ
            TicketService->>TicketService: computeReturn(em, ticketIds)
            alt ticketIds null/empty
                TicketService->>TicketService: throw IllegalArgumentException(TICKET_IDS_REQUIRED)
                TicketService-->>Socket_Router: Response.error(message)
            else ticketIds bị trùng (distinctIds.size != size)
                TicketService->>TicketService: throw IllegalArgumentException(TICKET_IDS_DUPLICATE)
                TicketService-->>Socket_Router: Response.error(message)
            else load tickets ok
                TicketService->>Repository: TicketRepository.findTicketsByIdsWithSchedule(em, distinctIds)
                Repository->>Database: JPQL SELECT Ticket JOIN FETCH customer/scheduleDetail/schedule WHERE id IN :ids
                Database-->>Repository: List<Ticket> tickets
                Repository-->>TicketService: tickets

                alt tickets.size != distinctIds.size
                    TicketService->>TicketService: throw IllegalArgumentException(SOME_TICKETS_INVALID)
                    TicketService-->>Socket_Router: Response.error(message)
                else duyệt từng ticket để tính hoàn tiền
                    loop for each ticket
                        alt ticket.status != PAID
                            TicketService->>TicketService: throw IllegalArgumentException(TICKET_NOT_RETURNABLE)
                        else scheduleDetail/schedule/departureTime null
                            TicketService->>TicketService: throw IllegalArgumentException(SCHEDULE_NOT_FOUND)
                        else minutesToDeparture < 4h
                            TicketService->>TicketService: throw IllegalArgumentException(NOT_ELIGIBLE_BY_TIME)
                        else hợp lệ
                            TicketService->>TicketService: feeRate = (minutesToDeparture < 24h ? 20% : 10%)
                            TicketService->>TicketService: fee = max(price*feeRate, 10_000); fee<=price
                            TicketService->>TicketService: refundAmount = price - fee
                        end
                    end
                    TicketService-->>Socket_Router: Response.success(PREVIEW_SUCCESS, ReturnTicketPreviewDTO{totalTicketPrice,refundFee,refundAmount})
                end
            end
        end
        Socket_Router-->>JavaFX_Client: Response
    end

    JavaFX_Client->>Socket_Router: Request(CONFIRM_RETURN_TICKETS, ReturnTicketConfirmDTO{ticketIds,refundAmount,employeeId})
    Socket_Router->>TicketService: confirmReturnTickets(confirmDTO)
    TicketService->>TicketService: ValidationUtils.validate(confirmDTO)
    alt dữ liệu không hợp lệ
        TicketService-->>Socket_Router: Response.error(DATA_INVALID + errors)
        Socket_Router-->>JavaFX_Client: Response.error
    else hợp lệ
        TicketService->>Database: EntityTransaction.begin()

        alt Happy path (không throw Exception)
            TicketService->>TicketService: computeReturn(em, confirmDTO.ticketIds)
            alt |confirmDTO.refundAmount - computed.totalRefundAmount| > 1.0
                note over TicketService,Database: Early return trong try; code KHÔNG rollback transaction một cách tường minh.
                TicketService-->>Socket_Router: Response.error(REFUND_AMOUNT_MISMATCH)
            else refundAmount khớp
                TicketService->>Repository: EmployeeRepository.findEmployeeById(em, employeeId)
                Repository->>Database: em.find(Employee, employeeId)
                Database-->>Repository: Employee? employee
                Repository-->>TicketService: employee

                alt employee == null
                    note over TicketService,Database: Early return; KHÔNG rollback tường minh.
                    TicketService-->>Socket_Router: Response.error(EmployeeMessages.notFoundById)
                else employee tồn tại
                    TicketService->>TicketService: check all tickets belong to same customer
                    alt khác customer trong danh sách ticketIds
                        note over TicketService,Database: Early return; KHÔNG rollback tường minh.
                        TicketService-->>Socket_Router: Response.error(CUSTOMER_MISMATCH)
                    else cùng customer
                        TicketService->>Repository: InvoiceRepository.createInvoice(em, Invoice{type=REFUND,totalAmount=totalRefundAmount,employee})
                        Repository->>Database: persist Invoice
                        Database-->>Repository: refundInvoiceId
                        Repository-->>TicketService: refundInvoice

                        loop for each ticket in computation.tickets
                            TicketService->>Repository: InvoiceDetailRepository.createInvoiceDetail(em, InvoiceDetail{isReturned=true,refundAmount,subTotal=ticketPrice})
                            Repository->>Database: persist InvoiceDetail
                            Database-->>Repository: ok
                            Repository-->>TicketService: ok
                        end

                        TicketService->>Repository: InvoiceDetailRepository.findInvoiceDetailsByTicketIdsAndInvoiceType(em, ticketIds, SALE)
                        Repository->>Database: JPQL SELECT InvoiceDetail JOIN FETCH invoice,ticket WHERE ticketId IN :ids AND invoice.type=SALE
                        Database-->>Repository: List<InvoiceDetail> saleDetails
                        Repository-->>TicketService: saleDetails

                        loop for each saleDetail
                            TicketService->>TicketService: saleDetail.returned=true; setRefundAmount(if ticketId match)
                        end
                        TicketService->>Repository: InvoiceDetailRepository.updateInvoiceDetails(em, saleDetails)
                        Repository->>Database: merge InvoiceDetail (for each)
                        Database-->>Repository: ok
                        Repository-->>TicketService: ok

                        loop for each ticket
                            TicketService->>TicketService: ticket.status=RETURNED; ticket.qrCode="INVALID"
                        end
                        TicketService->>Repository: TicketRepository.updateTickets(em, tickets)
                        Repository->>Database: merge Ticket (for each)
                        Database-->>Repository: ok
                        Repository-->>TicketService: ok

                        TicketService->>Database: EntityTransaction.commit()
                        TicketService-->>Socket_Router: Response.success(RETURN_SUCCESS, refundInvoiceId)
                    end
                end
            end

        else IllegalArgumentException từ computeReturn(...)
            TicketService->>Database: rollbackQuietly(tx).rollback()
            TicketService-->>Socket_Router: Response.error(e.message)

        else OptimisticLockException (xung đột dữ liệu)
            TicketService->>Database: rollbackQuietly(tx).rollback()
            TicketService-->>Socket_Router: Response.error(DATA_CONFLICT)

        else Exception khác
            TicketService->>Database: rollbackQuietly(tx).rollback()
            TicketService-->>Socket_Router: Response.error(RETURN_FAILED_PREFIX + e.message)
        end

        Socket_Router-->>JavaFX_Client: Response
    end
```

