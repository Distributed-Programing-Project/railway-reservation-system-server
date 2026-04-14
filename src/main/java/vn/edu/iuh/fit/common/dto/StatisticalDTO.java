package vn.edu.iuh.fit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticalDTO {

    private String routeId;
    private String routeName;

    private int totalTickets;
    private int totalTrips;
    private int soldTickets;

    private double occupancyRate;
    private BigDecimal revenue;
}