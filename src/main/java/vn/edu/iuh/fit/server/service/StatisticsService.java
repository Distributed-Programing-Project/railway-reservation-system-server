package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.common.dto.StatisticsRequestDTO;

public interface StatisticsService {

    Response getStatistics(StatisticsRequestDTO requestDTO);
}