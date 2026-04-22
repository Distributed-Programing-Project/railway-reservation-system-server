package vn.edu.iuh.fit.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticalDTO implements Serializable {

    private String routeId;
    private String routeName;

    private int totalTickets;
    private int totalTrips;
    private int soldTickets;

    private double occupancyRate;
    private BigDecimal revenue;
}
