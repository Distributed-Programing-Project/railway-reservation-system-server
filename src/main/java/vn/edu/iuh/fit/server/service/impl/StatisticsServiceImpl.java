package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.constant.EmployeeStatus;
import vn.edu.iuh.fit.common.constant.StatisticsPeriod;
import vn.edu.iuh.fit.common.dto.DailyRevenueDTO;
import vn.edu.iuh.fit.common.dto.StatisticsRequestDTO;
import vn.edu.iuh.fit.common.dto.StatisticsResultDTO;
import vn.edu.iuh.fit.common.message.StatisticsMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.model.Employee;
import vn.edu.iuh.fit.server.repository.EmployeeRepository;
import vn.edu.iuh.fit.server.repository.StatisticsRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.EmployeeRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.StatisticsRepositoryImpl;
import vn.edu.iuh.fit.server.service.StatisticsService;
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

        Response authError = requireActiveManager(requestDTO.getRequestEmployeeId());
        if (authError != null) return authError;

        String effectiveEmployeeId = resolveEffectiveEmployeeId(requestDTO.getRequestEmployeeId(), requestDTO.getEmployeeId());
        LocalDate[] dateRange = computeDateRange(requestDTO.getPeriodType(), requestDTO.getTargetDate());
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        try {
            StatisticsResultDTO result = buildResult(
                    requestDTO.getPeriodType(), startDate, endDate, start, end, effectiveEmployeeId);
            log.debug("getStatistics success: periodType={}, start={}, end={}", requestDTO.getPeriodType(), startDate, endDate);
            return Response.success(StatisticsMessages.GET_SUCCESS, result);
        } catch (Exception e) {
            log.error("Failed to get statistics: periodType={}, targetDate={}",
                    requestDTO.getPeriodType(), requestDTO.getTargetDate(), e);
            return Response.error(StatisticsMessages.SYSTEM_ERROR);
        }
    }

    private Response requireActiveManager(String employeeId) {
        return AbstractGenericRepositoryImpl.readOnly(em -> {
            Employee requester = employeeRepository.findEmployeeById(em, employeeId);
            if (requester == null) {
                return Response.error(String.format(StatisticsMessages.REQUEST_EMPLOYEE_NOT_FOUND, employeeId));
            }
            if (requester.getEmployeeStatus() != EmployeeStatus.ACTIVE) {
                return Response.error(StatisticsMessages.REQUEST_EMPLOYEE_INACTIVE);
            }
            if (!Boolean.TRUE.equals(requester.getIsManager())) {
                return Response.error(StatisticsMessages.NOT_MANAGER);
            }
            return null;
        });
    }

    private String resolveEffectiveEmployeeId(String requestEmployeeId, String filterEmployeeId) {
        return filterEmployeeId;
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
        List<Object[]> rows = statisticsRepository.aggregateDailyBreakdown(start, end, employeeId);
        Map<LocalDate, Object[]> rowsByDate = rows.stream()
                .collect(Collectors.toMap(row -> (LocalDate) row[0], row -> row));

        List<DailyRevenueDTO> breakdown = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Object[] row = rowsByDate.get(current);
            breakdown.add(row == null ? emptyDayRevenue(current) : mapRowToDailyRevenue(current, row));
            current = current.plusDays(1);
        }
        return breakdown;
    }

    private DailyRevenueDTO emptyDayRevenue(LocalDate day) {
        return DailyRevenueDTO.builder().day(day).revenue(BigDecimal.ZERO).ticketsSold(0).ticketsRefunded(0).build();
    }

    private DailyRevenueDTO mapRowToDailyRevenue(LocalDate day, Object[] row) {
        BigDecimal saleRevenue = toBigDecimal(row[1]);
        int sold = toInt(row[2]);
        BigDecimal refundAmount = toBigDecimal(row[3]);
        int refunded = toInt(row[4]);
        return DailyRevenueDTO.builder()
                .day(day)
                .revenue(saleRevenue.subtract(refundAmount))
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
