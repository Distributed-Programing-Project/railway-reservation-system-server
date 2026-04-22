package vn.edu.iuh.fit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.iuh.fit.common.constant.StatisticsPeriod;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticsResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private StatisticsPeriod periodType;
    private String periodLabel;
    private LocalDate startDate;
    private LocalDate endDate;

    private int totalTicketsSold;
    private int totalTicketsRefunded;

    private BigDecimal grossRevenue;
    private BigDecimal totalDiscount;
    private BigDecimal totalInsurance;
    private BigDecimal refundAmount;
    private BigDecimal netRevenue;

    private int exchangeCount;

    private List<DailyRevenueDTO> breakdown;
}
