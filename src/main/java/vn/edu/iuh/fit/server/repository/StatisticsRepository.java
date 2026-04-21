package vn.edu.iuh.fit.server.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsRepository {

    Object[] aggregateSaleStats(LocalDateTime start, LocalDateTime end, String employeeId);

    Object[] aggregateRefundStats(LocalDateTime start, LocalDateTime end, String employeeId);

    long countExchangeInvoices(LocalDateTime start, LocalDateTime end, String employeeId);

    List<Object[]> aggregateDailyBreakdown(LocalDateTime start, LocalDateTime end, String employeeId);
}
