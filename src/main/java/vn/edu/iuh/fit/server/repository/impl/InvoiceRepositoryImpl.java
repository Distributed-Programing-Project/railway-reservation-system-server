package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.server.model.Invoice;
import vn.edu.iuh.fit.server.repository.InvoiceRepository;

import java.util.List;

public class InvoiceRepositoryImpl extends AbstractGenericRepositoryImpl<Invoice, String> implements InvoiceRepository {

    public InvoiceRepositoryImpl() {
        super(Invoice.class);
    }

    @Override
    public Invoice createInvoice(EntityManager em, Invoice invoice) {
        em.persist(invoice);
        return invoice;
    }

    @Override
    public List<Invoice> filterInvoices(String keyword, Integer day, Integer month, Integer year,
            InvoiceType type, String employeeId, int page, int size) {
        return readOnly(em -> {
            StringBuilder jpql = new StringBuilder(
                "SELECT i FROM Invoice i " +
                "JOIN FETCH i.customer c " +
                "JOIN FETCH i.employee e " +
                "WHERE 1=1");
            appendWhereConditions(jpql, keyword, day, month, year, type, employeeId);
            jpql.append(" ORDER BY i.issueDate DESC");

            TypedQuery<Invoice> query = em.createQuery(jpql.toString(), Invoice.class);
            bindParameters(query, keyword, day, month, year, type, employeeId);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            List<Invoice> invoices = query.getResultList();
            invoices.forEach(invoice -> invoice.getDetails().size());
            return invoices;
        });
    }

    @Override
    public long countInvoices(String keyword, Integer day, Integer month, Integer year,
            InvoiceType type, String employeeId) {
        return readOnly(em -> {
            StringBuilder jpql = new StringBuilder(
                "SELECT COUNT(i) FROM Invoice i " +
                "JOIN i.customer c " +
                "JOIN i.employee e " +
                "WHERE 1=1");
            appendWhereConditions(jpql, keyword, day, month, year, type, employeeId);

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
            bindParameters(query, keyword, day, month, year, type, employeeId);
            return query.getSingleResult();
        });
    }

    @Override
    public Invoice findInvoiceWithHeader(String invoiceId) {
        return readOnly(em -> em.createQuery(
            "SELECT i FROM Invoice i " +
            "JOIN FETCH i.customer " +
            "JOIN FETCH i.employee " +
            "WHERE i.id = :invoiceId", Invoice.class)
            .setParameter("invoiceId", invoiceId)
            .getResultStream()
            .findFirst()
            .orElse(null));
    }

    private void appendWhereConditions(StringBuilder jpql, String keyword, Integer day, Integer month,
            Integer year, InvoiceType type, String employeeId) {
        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND (LOWER(i.id) LIKE :keyword OR LOWER(c.name) LIKE :keyword)");
        }
        if (day != null) jpql.append(" AND DAY(i.issueDate) = :day");
        if (month != null) jpql.append(" AND MONTH(i.issueDate) = :month");
        if (year != null) jpql.append(" AND YEAR(i.issueDate) = :year");
        if (type != null) jpql.append(" AND i.type = :type");
        if (employeeId != null) jpql.append(" AND i.employee.employeeId = :employeeId");
    }

    private <T> void bindParameters(TypedQuery<T> query, String keyword, Integer day, Integer month,
            Integer year, InvoiceType type, String employeeId) {
        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (day != null) query.setParameter("day", day);
        if (month != null) query.setParameter("month", month);
        if (year != null) query.setParameter("year", year);
        if (type != null) query.setParameter("type", type);
        if (employeeId != null) query.setParameter("employeeId", employeeId);
    }
}