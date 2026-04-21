package vn.edu.iuh.fit.server.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.constant.InvoiceType;
import vn.edu.iuh.fit.server.model.InvoiceDetail;

public interface InvoiceDetailRepository {
    InvoiceDetail createInvoiceDetail(EntityManager em, InvoiceDetail invoiceDetail);

    List<InvoiceDetail> findInvoiceDetailsByTicketIdsAndInvoiceType(EntityManager em, List<String> ticketIds,
            InvoiceType invoiceType);

    List<InvoiceDetail> updateInvoiceDetails(EntityManager em, List<InvoiceDetail> invoiceDetails);
}

