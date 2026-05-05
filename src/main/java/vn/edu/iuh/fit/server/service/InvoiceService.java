package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.dto.InvoiceFilterDTO;
import vn.edu.iuh.fit.common.response.Response;

public interface InvoiceService {
    Response filterInvoices(InvoiceFilterDTO filterDTO);
    Response getInvoiceDetailById(String invoiceId);
}
