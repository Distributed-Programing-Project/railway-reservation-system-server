package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Invoice;
import vn.edu.iuh.fit.server.repository.InvoiceRepository;

public class InvoiceRepositoryImpl extends AbstractGenericRepositoryImpl<Invoice, String> implements InvoiceRepository {

    public InvoiceRepositoryImpl() {
        super(Invoice.class);
    }

    @Override
    public Invoice createInvoice(EntityManager em, Invoice invoice) {
        em.persist(invoice);
        return invoice;
    }
}

