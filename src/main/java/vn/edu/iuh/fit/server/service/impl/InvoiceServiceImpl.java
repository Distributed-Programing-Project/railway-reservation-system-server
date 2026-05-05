package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.common.dto.InvoiceDetailResponseDTO;
import vn.edu.iuh.fit.common.dto.InvoiceFilterDTO;
import vn.edu.iuh.fit.common.dto.InvoiceLineItemDTO;
import vn.edu.iuh.fit.common.dto.InvoicePageDTO;
import vn.edu.iuh.fit.common.dto.InvoiceSummaryDTO;
import vn.edu.iuh.fit.common.dto.OriginalTicketInfoDTO;
import vn.edu.iuh.fit.common.message.InvoiceMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.Invoice;
import vn.edu.iuh.fit.server.model.InvoiceDetail;
import vn.edu.iuh.fit.server.model.Schedule;
import vn.edu.iuh.fit.server.model.ScheduleDetail;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.model.Ticket;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.InvoiceDetailRepository;
import vn.edu.iuh.fit.server.repository.InvoiceRepository;
import vn.edu.iuh.fit.server.repository.TicketRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.InvoiceDetailRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.InvoiceRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.TicketRepositoryImpl;
import vn.edu.iuh.fit.server.service.InvoiceService;
import vn.edu.iuh.fit.server.util.ValidationUtils;

import java.util.List;

public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    private final InvoiceRepository invoiceRepository = new InvoiceRepositoryImpl();
    private final InvoiceDetailRepository invoiceDetailRepository = new InvoiceDetailRepositoryImpl();
    private final TicketRepository ticketRepository = new TicketRepositoryImpl();
    private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();

    @Override
    public Response filterInvoices(InvoiceFilterDTO filterDTO) {
        List<String> errors = ValidationUtils.validate(filterDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Employee requestingEmployee = findActiveEmployee(filterDTO.getRequestEmployeeId());
        if (requestingEmployee == null) {
            return Response.error(InvoiceMessages.employeeNotFound(filterDTO.getRequestEmployeeId()));
        }
        if (requestingEmployee.getEmployeeStatus() != EmployeeStatus.ACTIVE) {
            return Response.error(InvoiceMessages.employeeInactive(filterDTO.getRequestEmployeeId()));
        }

        try {
            boolean isManager = Boolean.TRUE.equals(requestingEmployee.getIsManager());
            String scopeEmployeeId = isManager
                ? filterDTO.getFilterEmployeeId()
                : filterDTO.getRequestEmployeeId();

            List<Invoice> invoices = invoiceRepository.filterInvoices(
                filterDTO.getKeyword(),
                filterDTO.getDay(),
                filterDTO.getMonth(),
                filterDTO.getYear(),
                filterDTO.getType(),
                scopeEmployeeId,
                filterDTO.getPage(),
                filterDTO.getSize()
            );

            long totalElements = invoiceRepository.countInvoices(
                filterDTO.getKeyword(),
                filterDTO.getDay(),
                filterDTO.getMonth(),
                filterDTO.getYear(),
                filterDTO.getType(),
                scopeEmployeeId
            );

            int totalPages = filterDTO.getSize() > 0
                ? (int) Math.ceil((double) totalElements / filterDTO.getSize())
                : 0;

            List<InvoiceSummaryDTO> invoiceSummaryDTOs = invoices.stream()
                .map(this::toSummaryDTO)
                .toList();

            InvoicePageDTO invoicePageDTO = InvoicePageDTO.builder()
                .content(invoiceSummaryDTOs)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .currentPage(filterDTO.getPage())
                .build();

            return Response.success(InvoiceMessages.FILTER_SUCCESS, invoicePageDTO);
        } catch (Exception e) {
            log.error("filterInvoices failed: requestEmployeeId={}", filterDTO.getRequestEmployeeId(), e);
            return Response.error(InvoiceMessages.FILTER_FAILED + e.getMessage());
        }
    }

    @Override
    public Response getInvoiceDetailById(String invoiceId) {
        if (invoiceId == null || invoiceId.isBlank()) {
            return Response.error(InvoiceMessages.INVOICE_ID_REQUIRED);
        }

        try {
            Invoice invoice = invoiceRepository.findInvoiceWithHeader(invoiceId);
            if (invoice == null) {
                return Response.error(InvoiceMessages.notFound(invoiceId));
            }

            List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findDetailsWithFullChainByInvoiceId(invoiceId);

            List<InvoiceLineItemDTO> lineItems = invoiceDetails.stream()
                .map(detail -> toLineItemDTO(detail, invoice.getType(), invoiceId))
                .toList();

            InvoiceDetailResponseDTO invoiceDetailResponseDTO = InvoiceDetailResponseDTO.builder()
                .id(invoice.getId())
                .issueDate(invoice.getIssueDate())
                .totalAmount(invoice.getTotalAmount())
                .type(invoice.getType())
                .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
                .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : null)
                .customerPhoneNumber(invoice.getCustomer() != null ? invoice.getCustomer().getPhoneNumber() : null)
                .employeeId(invoice.getEmployee() != null ? invoice.getEmployee().getEmployeeId() : null)
                .employeeName(invoice.getEmployee() != null ? invoice.getEmployee().getEmployeeName() : null)
                .taxCode(invoice.getTaxCode())
                .companyName(invoice.getCompanyName())
                .details(lineItems)
                .build();

            return Response.success(InvoiceMessages.DETAIL_SUCCESS, invoiceDetailResponseDTO);
        } catch (Exception e) {
            log.error("getInvoiceDetailById failed: invoiceId={}", invoiceId, e);
            return Response.error(InvoiceMessages.DETAIL_FAILED + e.getMessage());
        }
    }

    private Employee findActiveEmployee(String employeeId) {
        return AbstractGenericRepositoryImpl.readOnly(em ->
            employeeRepository.findEmployeeById(em, employeeId));
    }

    private InvoiceSummaryDTO toSummaryDTO(Invoice invoice) {
        return InvoiceSummaryDTO.builder()
            .id(invoice.getId())
            .issueDate(invoice.getIssueDate())
            .totalAmount(invoice.getTotalAmount())
            .type(invoice.getType())
            .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : null)
            .employeeName(invoice.getEmployee() != null ? invoice.getEmployee().getEmployeeName() : null)
            .ticketCount(invoice.getDetails() != null ? invoice.getDetails().size() : 0)
            .build();
    }

    private InvoiceLineItemDTO toLineItemDTO(InvoiceDetail detail, InvoiceType invoiceType, String invoiceId) {
        Ticket ticket = detail.getTicket();
        ScheduleDetail scheduleDetail = ticket != null ? ticket.getScheduleDetail() : null;
        Seat seat = scheduleDetail != null ? scheduleDetail.getSeat() : null;
        Carriage carriage = seat != null ? seat.getCarriage() : null;
        Train train = carriage != null ? carriage.getTrain() : null;
        Schedule schedule = scheduleDetail != null ? scheduleDetail.getSchedule() : null;
        Station departureStation = scheduleDetail != null ? scheduleDetail.getSegmentDepartureStation() : null;
        Station arrivalStation = scheduleDetail != null ? scheduleDetail.getSegmentDestinationStation() : null;

        double finalAmount = (detail.getSubTotal() != null ? detail.getSubTotal() : 0.0)
            - detail.getDiscount() + detail.getInsurance();

        InvoiceLineItemDTO.InvoiceLineItemDTOBuilder lineItemBuilder = InvoiceLineItemDTO.builder()
            .id(detail.getId())
            .invoiceId(invoiceId)
            .ticketId(ticket != null ? ticket.getId() : null)
            .passengerName(ticket != null ? ticket.getPassengerName() : null)
            .passengerIdCard(ticket != null ? ticket.getPassengerIdCard() : null)
            .ticketType(ticket != null ? ticket.getType() : null)
            .trainName(train != null ? train.getTrainCode() : null)
            .carriageName(carriage != null ? String.valueOf(carriage.getNumber()) : null)
            .seatCode(seat != null ? String.valueOf(seat.getNumber()) : null)
            .departureStation(departureStation != null ? departureStation.getName() : null)
            .arrivalStation(arrivalStation != null ? arrivalStation.getName() : null)
            .departureTime(schedule != null ? schedule.getDepartureTime() : null)
            .subTotal(detail.getSubTotal())
            .discount(detail.getDiscount())
            .insurance(detail.getInsurance())
            .finalAmount(finalAmount)
            .isReturned(detail.isReturned())
            .refundAmount(detail.getRefundAmount());

        if (invoiceType == InvoiceType.EXCHANGE && ticket != null && ticket.getOriginalTicketId() != null) {
            lineItemBuilder.originalTicketInfo(resolveOriginalTicketInfo(ticket.getOriginalTicketId()));
        }

        return lineItemBuilder.build();
    }

    private OriginalTicketInfoDTO resolveOriginalTicketInfo(String originalTicketId) {
        try {
            Ticket originalTicket = ticketRepository.findOriginalTicketForDisplay(originalTicketId);
            if (originalTicket == null) {
                log.warn("Original ticket not found for EXCHANGE display: originalTicketId={}", originalTicketId);
                return null;
            }

            ScheduleDetail scheduleDetail = originalTicket.getScheduleDetail();
            Seat seat = scheduleDetail != null ? scheduleDetail.getSeat() : null;
            Carriage carriage = seat != null ? seat.getCarriage() : null;
            Train train = carriage != null ? carriage.getTrain() : null;
            Schedule schedule = scheduleDetail != null ? scheduleDetail.getSchedule() : null;
            Station departureStation = scheduleDetail != null ? scheduleDetail.getSegmentDepartureStation() : null;
            Station arrivalStation = scheduleDetail != null ? scheduleDetail.getSegmentDestinationStation() : null;

            return OriginalTicketInfoDTO.builder()
                .originalTicketId(originalTicket.getId())
                .originalTrainName(train != null ? train.getTrainCode() : null)
                .originalCarriageName(carriage != null ? String.valueOf(carriage.getNumber()) : null)
                .originalSeatCode(seat != null ? String.valueOf(seat.getNumber()) : null)
                .originalDepartureStation(departureStation != null ? departureStation.getName() : null)
                .originalArrivalStation(arrivalStation != null ? arrivalStation.getName() : null)
                .originalDepartureTime(schedule != null ? schedule.getDepartureTime() : null)
                .build();
        } catch (Exception e) {
            log.warn("Failed to resolve original ticket info: originalTicketId={}", originalTicketId, e);
            return null;
        }
    }
}
