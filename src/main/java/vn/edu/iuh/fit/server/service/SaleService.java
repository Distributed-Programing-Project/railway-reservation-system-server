package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.dto.SaleCreateRequestDTO;
import vn.edu.iuh.fit.common.dto.SaleScheduleSearchDTO;
import vn.edu.iuh.fit.common.dto.SeatHoldRequestDTO;
import vn.edu.iuh.fit.common.dto.SeatMapRequestDTO;
import vn.edu.iuh.fit.common.response.Response;

public interface SaleService {
  Response findAllStations();

  Response searchSchedulesForSale(SaleScheduleSearchDTO dto);

  Response getSeatMapForSchedule(SeatMapRequestDTO dto);

  Response holdSeatsForSale(SeatHoldRequestDTO dto);

  Response releaseHeldSeatsForSale(SeatHoldRequestDTO dto);

  Response createSaleTransaction(SaleCreateRequestDTO dto);
}
