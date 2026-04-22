package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.constant.CarriageType;
import vn.edu.iuh.fit.server.constant.SeatType;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Seat;
import vn.edu.iuh.fit.server.repository.CarriageRepository;

import java.util.List;
import java.util.Map;

public class CarriageRepositoryImpl extends AbstractGenericRepositoryImpl<Carriage, String>
        implements CarriageRepository {

    private static final Map<CarriageType, Integer> SEAT_COUNT_BY_TYPE = Map.of(
            CarriageType.HARD_SEAT, 64,
            CarriageType.SOFT_SEAT, 56,
            CarriageType.SOFT_SEAT_AC, 56,
            CarriageType.BERTH_6, 42,
            CarriageType.BERTH_4, 36
    );

    private static final Map<CarriageType, SeatType> SEAT_TYPE_BY_CARRIAGE = Map.of(
            CarriageType.HARD_SEAT, SeatType.HARD_SEAT,
            CarriageType.SOFT_SEAT, SeatType.SOFT_SEAT,
            CarriageType.SOFT_SEAT_AC, SeatType.VIP_SEAT,
            CarriageType.BERTH_6, SeatType.BERTH_6,
            CarriageType.BERTH_4, SeatType.BERTH_4
    );

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

        SeatType seatType = SEAT_TYPE_BY_CARRIAGE.get(type);
        int seatCount = SEAT_COUNT_BY_TYPE.get(type);

        for (int i = 1; i <= seatCount; i++) {
            em.persist(Seat.builder()
                    .number(i)
                    .type(seatType)
                    .carriage(carriage)
                    .build());
        }
        return carriage;
    }
}
