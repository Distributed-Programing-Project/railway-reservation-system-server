package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.server.model.Invoice;

import java.util.List;

public interface InvoiceRepository {
    Invoice createInvoice(EntityManager em, Invoice invoice);

    List<Invoice> filterInvoices(String keyword, Integer day, Integer month, Integer year,
            InvoiceType type, String employeeId, int page, int size);

    long countInvoices(String keyword, Integer day, Integer month, Integer year,
            InvoiceType type, String employeeId);

    Invoice findInvoiceWithHeader(String invoiceId);
}

