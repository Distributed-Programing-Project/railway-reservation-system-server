package vn.edu.iuh.fit.server.service.impl;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.constant.InvoiceType;
import vn.edu.iuh.fit.server.constant.StatisticsPeriod;
import vn.edu.iuh.fit.server.dto.DailyRevenueDTO;
import vn.edu.iuh.fit.server.dto.StatisticsRequestDTO;
import vn.edu.iuh.fit.server.dto.StatisticsResultDTO;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.model.InvoiceDetail;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.StatisticsRepository;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.StatisticsRepositoryImpl;
import vn.edu.iuh.fit.server.service.StatisticsService;
import vn.edu.iuh.fit.server.util.JPAUtils;
import vn.edu.iuh.fit.server.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private final StatisticsRepository statisticsRepository = new StatisticsRepositoryImpl();
    private final EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();

    @Override
    public Response getStatistics(StatisticsRequestDTO requestDTO) {
        List<String> errors = ValidationUtils.validate(requestDTO);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        Employee requester = findRequester(requestDTO.getRequestEmployeeId());
        if (requester == null) {
            return Response.error("Nhân viên yêu cầu không tồn tại: " + requestDTO.getRequestEmployeeId());
        }

        String effectiveEmployeeId = resolveEffectiveEmployeeId(requester, requestDTO.getEmployeeId());
        LocalDate[] dateRange = computeDateRange(requestDTO.getPeriodType(), requestDTO.getTargetDate());
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        try {
            StatisticsResultDTO result = buildResult(
                    requestDTO.getPeriodType(), startDate, endDate, start, end, effectiveEmployeeId);
            log.debug("getStatistics success: periodType={}, start={}, end={}", requestDTO.getPeriodType(), startDate, endDate);
            return Response.success("Thống kê thành công", result);
        } catch (Exception e) {
            log.error("Failed to get statistics: periodType={}, targetDate={}",
                    requestDTO.getPeriodType(), requestDTO.getTargetDate(), e);
            return Response.error("Lỗi hệ thống khi truy vấn thống kê");
        }
    }

    private Employee findRequester(String requestEmployeeId) {
        EntityManager em = JPAUtils.getEntityManager();
        try {
            return employeeRepository.findEmployeeById(em, requestEmployeeId);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private String resolveEffectiveEmployeeId(Employee requester, String filterEmployeeId) {
        if (Boolean.TRUE.equals(requester.getIsManager())) {
            return filterEmployeeId;
        }
        return requester.getEmployeeId();
    }

    private LocalDate[] computeDateRange(StatisticsPeriod periodType, LocalDate targetDate) {
        return switch (periodType) {
            case DAY -> new LocalDate[]{targetDate, targetDate};
            case WEEK -> {
                LocalDate monday = targetDate.with(DayOfWeek.MONDAY);
                yield new LocalDate[]{monday, monday.plusDays(6)};
            }
            case MONTH -> {
                LocalDate firstDay = targetDate.withDayOfMonth(1);
                yield new LocalDate[]{firstDay, firstDay.with(TemporalAdjusters.lastDayOfMonth())};
            }
        };
    }

    private String buildPeriodLabel(StatisticsPeriod periodType, LocalDate startDate) {
        return switch (periodType) {
            case DAY -> startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case WEEK -> {
                int weekNumber = startDate.get(WeekFields.ISO.weekOfWeekBasedYear());
                int year = startDate.get(WeekFields.ISO.weekBasedYear());
                yield "Tuần " + weekNumber + "/" + year;
            }
            case MONTH -> "Tháng " + startDate.getMonthValue() + "/" + startDate.getYear();
        };
    }

    private StatisticsResultDTO buildResult(StatisticsPeriod periodType, LocalDate startDate, LocalDate endDate,
                                             LocalDateTime start, LocalDateTime end, String employeeId) {
        Object[] saleStats = statisticsRepository.aggregateSaleStats(start, end, employeeId);
        Object[] refundStats = statisticsRepository.aggregateRefundStats(start, end, employeeId);
        long exchangeCount = statisticsRepository.countExchangeInvoices(start, end, employeeId);

        int totalTicketsSold = toInt(saleStats[0]);
        BigDecimal grossRevenue = toBigDecimal(saleStats[1]);
        BigDecimal totalDiscount = toBigDecimal(saleStats[2]);
        BigDecimal totalInsurance = toBigDecimal(saleStats[3]);
        int totalTicketsRefunded = toInt(refundStats[0]);
        BigDecimal refundAmount = toBigDecimal(refundStats[1]);
        BigDecimal netRevenue = grossRevenue.subtract(refundAmount);

        List<DailyRevenueDTO> breakdown = null;
        if (periodType == StatisticsPeriod.WEEK || periodType == StatisticsPeriod.MONTH) {
            breakdown = buildDailyBreakdown(start, end, startDate, endDate, employeeId);
        }

        return StatisticsResultDTO.builder()
                .periodType(periodType)
                .periodLabel(buildPeriodLabel(periodType, startDate))
                .startDate(startDate)
                .endDate(endDate)
                .totalTicketsSold(totalTicketsSold)
                .totalTicketsRefunded(totalTicketsRefunded)
                .grossRevenue(grossRevenue)
                .totalDiscount(totalDiscount)
                .totalInsurance(totalInsurance)
                .refundAmount(refundAmount)
                .netRevenue(netRevenue)
                .exchangeCount(Math.toIntExact(exchangeCount))
                .breakdown(breakdown)
                .build();
    }

    private List<DailyRevenueDTO> buildDailyBreakdown(LocalDateTime start, LocalDateTime end,
                                                        LocalDate startDate, LocalDate endDate,
                                                        String employeeId) {
        List<InvoiceDetail> invoiceDetails = statisticsRepository.findInvoiceDetailsInPeriod(start, end, employeeId);

        Map<LocalDate, List<InvoiceDetail>> detailsByDate = invoiceDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getInvoice().getIssueDate().toLocalDate()));

        List<DailyRevenueDTO> breakdown = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<InvoiceDetail> dayDetails = detailsByDate.getOrDefault(current, List.of());
            breakdown.add(aggregateDailyRevenue(current, dayDetails));
            current = current.plusDays(1);
        }
        return breakdown;
    }

    private DailyRevenueDTO aggregateDailyRevenue(LocalDate day, List<InvoiceDetail> dayDetails) {
        int sold = 0;
        int refunded = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        for (InvoiceDetail detail : dayDetails) {
            if (detail.isReturned()) {
                refunded++;
                revenue = revenue.subtract(BigDecimal.valueOf(detail.getRefundAmount()));
            } else if (detail.getInvoice().getType() == InvoiceType.SALE) {
                sold++;
                revenue = revenue.add(BigDecimal.valueOf(detail.getSubTotal()));
            }
        }

        return DailyRevenueDTO.builder()
                .day(day)
                .revenue(revenue)
                .ticketsSold(sold)
                .ticketsRefunded(refunded)
                .build();
    }

    private int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bigDecimal) return bigDecimal;
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }
}