package vn.edu.iuh.fit.qa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import vn.edu.iuh.fit.client.service.SaleClientService;
import vn.edu.iuh.fit.common.constant.SeatAvailabilityStatus;
import vn.edu.iuh.fit.common.constant.TicketCategory;
import vn.edu.iuh.fit.common.dto.SaleScheduleSearchDTO;
import vn.edu.iuh.fit.common.dto.SaleScheduleSearchResultDTO;
import vn.edu.iuh.fit.common.dto.ScheduleSaleCardDTO;
import vn.edu.iuh.fit.common.dto.SeatHoldResponseDTO;
import vn.edu.iuh.fit.common.dto.SeatMapResponseDTO;
import vn.edu.iuh.fit.common.dto.SeatMapSeatDTO;
import vn.edu.iuh.fit.common.dto.StationDTO;
import vn.edu.iuh.fit.common.response.Response;

/**
 * Best-effort E2E socket smoke test for SeatMap + hold/release.
 *
 * Skips automatically if:
 * - server is not reachable on 127.0.0.1:9090
 * - no stations/schedules/seats available in current DB seed
 */
public class SaleSeatHoldE2eSmokeTest {

  @Test
  void canGetSeatMapHoldAndRelease() {
    Assumptions.assumeTrue(isPortOpen("127.0.0.1", 9090, 500), "Server not reachable on 127.0.0.1:9090");

    SaleClientService svc = new SaleClientService();
    Response stationRes = svc.findAllStations();
    Assumptions.assumeTrue(stationRes != null && stationRes.isSuccess(), "findAllStations failed: " + safeMsg(stationRes));
    Assumptions.assumeTrue(stationRes.getData() instanceof List<?>, "Unexpected stations payload");
    List<?> stations = (List<?>) stationRes.getData();
    Assumptions.assumeTrue(stations.size() >= 2, "Not enough stations to run test");

    StationDTO a = (StationDTO) stations.get(0);
    StationDTO b = (StationDTO) stations.get(1);
    Assumptions.assumeTrue(a != null && b != null && !Objects.equals(a.getId(), b.getId()), "Invalid stations from server");

    SaleScheduleSearchDTO search = SaleScheduleSearchDTO.builder()
        .departureStationId(a.getId())
        .destinationStationId(b.getId())
        .departureDate(LocalDate.now())
        .ticketCategory(TicketCategory.ONE_WAY)
        .page(0)
        .size(5)
        .build();

    Response searchRes = svc.searchSchedulesForSale(search);
    Assumptions.assumeTrue(searchRes != null && searchRes.isSuccess(), "searchSchedulesForSale failed: " + safeMsg(searchRes));
    Assumptions.assumeTrue(searchRes.getData() instanceof SaleScheduleSearchResultDTO, "Unexpected search result payload");
    SaleScheduleSearchResultDTO result = (SaleScheduleSearchResultDTO) searchRes.getData();
    Assumptions.assumeTrue(result.getOutboundSchedules() != null && !result.getOutboundSchedules().isEmpty(),
        "No outbound schedules");

    ScheduleSaleCardDTO schedule = result.getOutboundSchedules().get(0);
    Assumptions.assumeTrue(schedule != null && schedule.getScheduleId() != null, "Invalid schedule payload");

    String clientSessionId = UUID.randomUUID().toString();
    Response seatMapRes = svc.getSeatMap(schedule.getScheduleId(), clientSessionId);
    Assumptions.assumeTrue(seatMapRes != null && seatMapRes.isSuccess(), "getSeatMap failed: " + safeMsg(seatMapRes));
    Assumptions.assumeTrue(seatMapRes.getData() instanceof SeatMapResponseDTO, "Unexpected seat map payload");
    SeatMapResponseDTO sm = (SeatMapResponseDTO) seatMapRes.getData();
    Assumptions.assumeTrue(sm.getCarriages() != null && !sm.getCarriages().isEmpty(),
        "No carriages in seat map");

    SeatMapSeatDTO candidate = sm.getCarriages().stream()
        .filter(c -> c.getSeats() != null)
        .flatMap(c -> c.getSeats().stream())
        .filter(s -> s != null && s.getScheduleDetailId() != null)
        .filter(s -> s.getSeatStatus() == SeatAvailabilityStatus.AVAILABLE)
        .findFirst()
        .orElse(null);

    Assumptions.assumeTrue(candidate != null, "No AVAILABLE seat found in seat map");

    Response holdRes = svc.holdSeats(schedule.getScheduleId(), List.of(candidate.getScheduleDetailId()), clientSessionId);
    Assumptions.assumeTrue(holdRes != null && holdRes.isSuccess(), "holdSeats failed: " + safeMsg(holdRes));
    Assumptions.assumeTrue(holdRes.getData() instanceof SeatHoldResponseDTO, "Unexpected hold response payload");
    SeatHoldResponseDTO holdDto = (SeatHoldResponseDTO) holdRes.getData();
    assertTrue(holdDto.getSuccessIds() != null && holdDto.getSuccessIds().contains(candidate.getScheduleDetailId()),
        "holdSeats did not report success for the candidate seat");

    Response releaseRes = svc.releaseHeldSeats(schedule.getScheduleId(), List.of(candidate.getScheduleDetailId()), clientSessionId);
    Assumptions.assumeTrue(releaseRes != null && releaseRes.isSuccess(), "releaseHeldSeats failed: " + safeMsg(releaseRes));
  }

  private static boolean isPortOpen(String host, int port, int timeoutMs) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeoutMs);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static String safeMsg(Response res) {
    return res == null ? "no response" : String.valueOf(res.getMessage());
  }
}
