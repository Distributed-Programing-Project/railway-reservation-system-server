package vn.edu.iuh.fit.server.repository;

import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.ScheduleDetail;

public interface ScheduleDetailRepository {

    Set<String> getSoldSeatIds(EntityManager em, String scheduleId);

    Set<String> getSoldSeatIdsWithLock(EntityManager em, String scheduleId);

    ScheduleDetail findById(String id, EntityManager em);

    List<ScheduleDetail> findByIdsWithSeatAndSchedule(EntityManager em, List<String> ids);

    List<ScheduleDetail> findByScheduleIdWithSeat(EntityManager em, String scheduleId);

    Set<String> findDetailIdsInSchedule(EntityManager em, String scheduleId, Set<String> detailIds);

    Set<String> getSoldScheduleDetailIds(EntityManager em, String scheduleId);

    ScheduleDetail updateScheduleDetail(EntityManager em, ScheduleDetail scheduleDetail);

    boolean existsUnpricedSeat(EntityManager em, String scheduleId);

    void updatePrices(EntityManager em, Map<String, BigDecimal> pricesByScheduleDetailId);
}
