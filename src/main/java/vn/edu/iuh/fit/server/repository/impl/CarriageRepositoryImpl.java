package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.constant.CarriageType;
import vn.edu.iuh.fit.common.constant.SeatType;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.repository.CarriageRepository;

import java.util.List;

public class CarriageRepositoryImpl extends AbstractGenericRepositoryImpl<Carriage, String>
        implements CarriageRepository {

    public CarriageRepositoryImpl() {
        super(Carriage.class);
    }

    @Override
    public List<Carriage> findUnassignedCarriages() {
        return doWithEntityManager(em ->
                em.createQuery(
                        "SELECT c FROM Carriage c WHERE c.train IS NULL ORDER BY c.type",
                        Carriage.class)
                        .getResultList()
        );
    }

    @Override
    public List<Carriage> findCarriagesByIds(List<String> carriageIds) {
        if (carriageIds == null || carriageIds.isEmpty()) return List.of();
        return doWithEntityManager(em ->
                em.createQuery(
                        "SELECT c FROM Carriage c WHERE c.id IN :ids",
                        Carriage.class)
                        .setParameter("ids", carriageIds)
                        .getResultList()
        );
    }

    @Override
    public List<Carriage> findCarriagesByTrainId(String trainId) {
        return doWithEntityManager(em ->
                em.createQuery(
                        "SELECT c FROM Carriage c WHERE c.train.id = :trainId ORDER BY c.number",
                        Carriage.class)
                        .setParameter("trainId", trainId)
                        .getResultList()
        );
    }

    @Override
    public Carriage saveCarriageWithSeats(EntityManager em, CarriageType type) {
        Carriage carriage = Carriage.builder()
                .type(type)
                .number(0)
                .build();
        em.persist(carriage);

        SeatType seatType = type.getSeatType();
        int seatCount = type.getSeatCount();

        for (int i = 1; i <= seatCount; i++) {
            em.persist(Seat.builder()
                    .number(i)
                    .type(seatType)
                    .carriage(carriage)
                    .available(true)
                    .build());
        }
        return carriage;
    }
}
