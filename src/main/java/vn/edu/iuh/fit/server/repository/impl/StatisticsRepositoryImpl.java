package vn.edu.iuh.fit.server.repository.impl;

import vn.edu.iuh.fit.server.constant.InvoiceType;
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
        return doWithEntityManager(em -> {
            if (employeeId != null) {
                return em.createQuery(
                        "SELECT COUNT(id), SUM(id.subTotal), SUM(id.discount), SUM(id.insurance) " +
                        "FROM InvoiceDetail id JOIN id.invoice inv " +
                        "WHERE inv.type = :type AND inv.issueDate >= :start AND inv.issueDate <= :end " +
                        "AND inv.employee.employeeId = :empId", Object[].class)
                        .setParameter("type", InvoiceType.SALE)
                        .setParameter("start", start)
                        .setParameter("end", end)
                        .setParameter("empId", employeeId)
                        .getSingleResult();
            }
            return em.createQuery(
                    "SELECT COUNT(id), SUM(id.subTotal), SUM(id.discount), SUM(id.insurance) " +
                    "FROM InvoiceDetail id JOIN id.invoice inv " +
                    "WHERE inv.type = :type AND inv.issueDate >= :start AND inv.issueDate <= :end", Object[].class)
                    .setParameter("type", InvoiceType.SALE)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        });
    }

    @Override
    public Object[] aggregateRefundStats(LocalDateTime start, LocalDateTime end, String employeeId) {
        return doWithEntityManager(em -> {
            if (employeeId != null) {
                return em.createQuery(
                        "SELECT COUNT(id), SUM(id.refundAmount) " +
                        "FROM InvoiceDetail id JOIN id.invoice inv " +
                        "WHERE id.isReturned = true AND inv.issueDate >= :start AND inv.issueDate <= :end " +
                        "AND inv.employee.employeeId = :empId", Object[].class)
                        .setParameter("start", start)
                        .setParameter("end", end)
                        .setParameter("empId", employeeId)
                        .getSingleResult();
            }
            return em.createQuery(
                    "SELECT COUNT(id), SUM(id.refundAmount) " +
                    "FROM InvoiceDetail id JOIN id.invoice inv " +
                    "WHERE id.isReturned = true AND inv.issueDate >= :start AND inv.issueDate <= :end", Object[].class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        });
    }

    @Override
    public long countExchangeInvoices(LocalDateTime start, LocalDateTime end, String employeeId) {
        return doWithEntityManager(em -> {
            if (employeeId != null) {
                return em.createQuery(
                        "SELECT COUNT(inv) FROM Invoice inv " +
                        "WHERE inv.type = :type AND inv.issueDate >= :start AND inv.issueDate <= :end " +
                        "AND inv.employee.employeeId = :empId", Long.class)
                        .setParameter("type", InvoiceType.EXCHANGE)
                        .setParameter("start", start)
                        .setParameter("end", end)
                        .setParameter("empId", employeeId)
                        .getSingleResult();
            }
            return em.createQuery(
                    "SELECT COUNT(inv) FROM Invoice inv " +
                    "WHERE inv.type = :type AND inv.issueDate >= :start AND inv.issueDate <= :end", Long.class)
                    .setParameter("type", InvoiceType.EXCHANGE)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        });
    }

    @Override
    public List<InvoiceDetail> findInvoiceDetailsInPeriod(LocalDateTime start, LocalDateTime end, String employeeId) {
        return doWithEntityManager(em -> {
            if (employeeId != null) {
                return em.createQuery(
                        "SELECT id FROM InvoiceDetail id JOIN FETCH id.invoice inv " +
                        "WHERE inv.issueDate >= :start AND inv.issueDate <= :end " +
                        "AND inv.employee.employeeId = :empId", InvoiceDetail.class)
                        .setParameter("start", start)
                        .setParameter("end", end)
                        .setParameter("empId", employeeId)
                        .getResultList();
            }
            return em.createQuery(
                    "SELECT id FROM InvoiceDetail id JOIN FETCH id.invoice inv " +
                    "WHERE inv.issueDate >= :start AND inv.issueDate <= :end", InvoiceDetail.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getResultList();
        });
    }
}
