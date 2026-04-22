package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.TypedQuery;
import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.server.model.InvoiceDetail;
import vn.edu.iuh.fit.server.repository.StatisticsRepository;

import java.time.LocalDateTime;
import java.util.List;

public class StatisticsRepositoryImpl extends AbstractGenericRepositoryImpl<InvoiceDetail, String>
        implements StatisticsRepository {

    public StatisticsRepositoryImpl() {
        super(InvoiceDetail.class);
    }

    @Override
    public Object[] aggregateSaleStats(LocalDateTime start, LocalDateTime end, String employeeId) {
        return doWithEntityManager(em ->
            bindCommonParams(em.createQuery(
                "SELECT COUNT(invDetail), SUM(invDetail.subTotal), SUM(invDetail.discount), SUM(invDetail.insurance) " +
                "FROM InvoiceDetail invDetail JOIN invDetail.invoice inv " +
                "WHERE inv.type = :type AND inv.issueDate >= :start AND inv.issueDate <= :end" +
                employeeClause(employeeId), Object[].class)
                .setParameter("type", InvoiceType.SALE), start, end, employeeId)
            .getSingleResult()
        );
    }

    @Override
    public Object[] aggregateRefundStats(LocalDateTime start, LocalDateTime end, String employeeId) {
        return doWithEntityManager(em ->
            bindCommonParams(em.createQuery(
                "SELECT COUNT(invDetail), SUM(invDetail.refundAmount) " +
                "FROM InvoiceDetail invDetail JOIN invDetail.invoice inv " +
                "WHERE invDetail.isReturned = true AND inv.issueDate >= :start AND inv.issueDate <= :end" +
                employeeClause(employeeId), Object[].class), start, end, employeeId)
            .getSingleResult()
        );
    }

    @Override
    public long countExchangeInvoices(LocalDateTime start, LocalDateTime end, String employeeId) {
        return doWithEntityManager(em ->
            bindCommonParams(em.createQuery(
                "SELECT COUNT(inv) FROM Invoice inv " +
                "WHERE inv.type = :type AND inv.issueDate >= :start AND inv.issueDate <= :end" +
                employeeClause(employeeId), Long.class)
                .setParameter("type", InvoiceType.EXCHANGE), start, end, employeeId)
            .getSingleResult()
        );
    }

    @Override
    public List<Object[]> aggregateDailyBreakdown(LocalDateTime start, LocalDateTime end, String employeeId) {
        return doWithEntityManager(em ->
            bindCommonParams(em.createQuery(
                "SELECT cast(inv.issueDate as LocalDate), " +
                "SUM(CASE WHEN invDetail.isReturned = false AND inv.type = :saleType THEN invDetail.subTotal ELSE 0.0 END), " +
                "SUM(CASE WHEN invDetail.isReturned = false AND inv.type = :saleType THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN invDetail.isReturned = true THEN invDetail.refundAmount ELSE 0.0 END), " +
                "SUM(CASE WHEN invDetail.isReturned = true THEN 1 ELSE 0 END) " +
                "FROM InvoiceDetail invDetail JOIN invDetail.invoice inv " +
                "WHERE inv.issueDate >= :start AND inv.issueDate <= :end" +
                employeeClause(employeeId) +
                " GROUP BY cast(inv.issueDate as LocalDate) ORDER BY cast(inv.issueDate as LocalDate)", Object[].class)
                .setParameter("saleType", InvoiceType.SALE), start, end, employeeId)
            .getResultList()
        );
    }

    private String employeeClause(String employeeId) {
        return employeeId != null ? " AND inv.employee.employeeId = :empId" : "";
    }

    private <T> TypedQuery<T> bindCommonParams(TypedQuery<T> query,
                                                LocalDateTime start, LocalDateTime end, String employeeId) {
        query.setParameter("start", start).setParameter("end", end);
        if (employeeId != null) query.setParameter("empId", employeeId);
        return query;
    }
}
