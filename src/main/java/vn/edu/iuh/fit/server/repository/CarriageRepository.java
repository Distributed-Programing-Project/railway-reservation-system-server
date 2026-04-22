package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.constant.CarriageType;
import vn.edu.iuh.fit.server.model.Carriage;

import java.util.List;

public interface CarriageRepository {
    List<Carriage> findUnassignedCarriages();
    List<Carriage> findCarriagesByIds(List<String> carriageIds);
    List<Carriage> findCarriagesByTrainId(String trainId);
    Carriage saveCarriageWithSeats(EntityManager em, CarriageType type);
}
