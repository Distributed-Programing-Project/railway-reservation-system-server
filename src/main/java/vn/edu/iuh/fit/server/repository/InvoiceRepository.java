package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Invoice;

public interface InvoiceRepository {
    Invoice createInvoice(EntityManager em, Invoice invoice);
}

