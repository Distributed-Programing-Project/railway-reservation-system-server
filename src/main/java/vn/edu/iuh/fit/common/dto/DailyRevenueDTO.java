package vn.edu.iuh.fit.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRevenueDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate day;
    private BigDecimal revenue;
    private int ticketsSold;
    private int ticketsRefunded;
}
