package vn.edu.iuh.fit.server.service;

import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.dto.CreateCarriageDTO;
import vn.edu.iuh.fit.server.dto.CreateTrainDTO;
import vn.edu.iuh.fit.server.dto.TrainFilterDTO;
import vn.edu.iuh.fit.server.dto.UpdateTrainCarriagesDTO;
import vn.edu.iuh.fit.server.dto.UpdateTrainStatusDTO;

public interface TrainService {
    Response findAllTrains(TrainFilterDTO filter);
    Response findTrainsByCode(String keyword);
    Response findUnassignedCarriages();
    Response createTrain(CreateTrainDTO dto);
    Response createCarriage(CreateCarriageDTO dto);
    Response updateTrainCarriages(UpdateTrainCarriagesDTO dto);
    Response updateTrainStatus(UpdateTrainStatusDTO dto);
}
