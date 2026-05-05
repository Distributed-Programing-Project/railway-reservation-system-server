package vn.edu.iuh.fit.server.repository.impl;

import java.util.List;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.InvoiceType;
import vn.edu.iuh.fit.server.model.InvoiceDetail;
import vn.edu.iuh.fit.server.repository.InvoiceDetailRepository;

public class InvoiceDetailRepositoryImpl extends AbstractGenericRepositoryImpl<InvoiceDetail, String>
        implements InvoiceDetailRepository {

    public InvoiceDetailRepositoryImpl() {
        super(InvoiceDetail.class);
    }

    @Override
    public InvoiceDetail createInvoiceDetail(EntityManager em, InvoiceDetail invoiceDetail) {
        em.persist(invoiceDetail);
        return invoiceDetail;
    }

    @Override
    public List<InvoiceDetail> findInvoiceDetailsByTicketIdsAndInvoiceType(EntityManager em, List<String> ticketIds,
            InvoiceType invoiceType) {
        String jpql = "SELECT d FROM InvoiceDetail d " +
                "JOIN FETCH d.invoice i " +
                "LEFT JOIN FETCH i.customer " +
                "LEFT JOIN FETCH i.employee " +
                "JOIN FETCH d.ticket " +
                "WHERE d.ticket.id IN :ticketIds AND i.type = :invoiceType";
        return em.createQuery(jpql, InvoiceDetail.class)
                .setParameter("ticketIds", ticketIds)
                .setParameter("invoiceType", invoiceType)
                .getResultList();
    }

    @Override
    public List<InvoiceDetail> updateInvoiceDetails(EntityManager em, List<InvoiceDetail> invoiceDetails) {
        if (invoiceDetails == null || invoiceDetails.isEmpty()) {
            return List.of();
        }
        for (InvoiceDetail detail : invoiceDetails) {
            em.merge(detail);
        }
        return invoiceDetails;
    }

    @Override
    public List<InvoiceDetail> findDetailsWithFullChainByInvoiceId(String invoiceId) {
        return readOnly(em -> em.createQuery(
            "SELECT d FROM InvoiceDetail d " +
            "JOIN FETCH d.ticket t " +
            "LEFT JOIN FETCH t.scheduleDetail sd " +
            "LEFT JOIN FETCH sd.schedule " +
            "LEFT JOIN FETCH sd.seat seat " +
            "LEFT JOIN FETCH seat.carriage car " +
            "LEFT JOIN FETCH car.train " +
            "LEFT JOIN FETCH sd.segmentDepartureStation " +
            "LEFT JOIN FETCH sd.segmentDestinationStation " +
            "WHERE d.invoice.id = :invoiceId", InvoiceDetail.class)
            .setParameter("invoiceId", invoiceId)
            .getResultList());
    }
}

