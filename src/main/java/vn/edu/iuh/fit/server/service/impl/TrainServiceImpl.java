package vn.edu.iuh.fit.server.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.iuh.fit.common.constant.TrainStatus;
import vn.edu.iuh.fit.common.dto.CarriageDTO;
import vn.edu.iuh.fit.common.dto.CreateCarriageDTO;
import vn.edu.iuh.fit.common.dto.CreateTrainDTO;
import vn.edu.iuh.fit.common.dto.TrainDTO;
import vn.edu.iuh.fit.common.dto.TrainFilterDTO;
import vn.edu.iuh.fit.common.dto.UpdateTrainCarriagesDTO;
import vn.edu.iuh.fit.common.dto.UpdateTrainStatusDTO;
import vn.edu.iuh.fit.common.message.TrainMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.mapper.CarriageMapper;
import vn.edu.iuh.fit.server.mapper.TrainMapper;
import vn.edu.iuh.fit.server.model.Carriage;
import vn.edu.iuh.fit.server.model.Train;
import vn.edu.iuh.fit.server.repository.CarriageRepository;
import vn.edu.iuh.fit.server.repository.ScheduleRepository;
import vn.edu.iuh.fit.server.repository.TrainRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.CarriageRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.ScheduleRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.TrainRepositoryImpl;
import vn.edu.iuh.fit.server.service.TrainService;
import vn.edu.iuh.fit.server.util.ValidationUtils;

import java.util.List;

public class TrainServiceImpl implements TrainService {

    private static final Logger log = LoggerFactory.getLogger(TrainServiceImpl.class);

    private final TrainRepository trainRepository = new TrainRepositoryImpl();
    private final CarriageRepository carriageRepository = new CarriageRepositoryImpl();
    private final ScheduleRepository scheduleRepository = new ScheduleRepositoryImpl();

    @Override
    public Response findAllTrains(TrainFilterDTO filter) {
        List<String> errors = ValidationUtils.validate(filter);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }
        try {
            List<Train> trains = trainRepository.findAllTrains(filter.getStatusFilter());
            List<TrainDTO> trainDTOList = TrainMapper.INSTANCE.toDtoList(trains);
            return Response.success(TrainMessages.FIND_ALL_SUCCESS, trainDTOList);
        } catch (RuntimeException e) {
            log.error("Failed to find all trains", e);
            return Response.error(TrainMessages.SYSTEM_ERROR);
        }
    }

    @Override
    public Response findTrainsByCode(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Response.error(TrainMessages.FIND_BY_CODE_BLANK);
        }
    List<Train> trains = trainRepository.findTrainsByCodeLike(keyword);
    List<TrainDTO> trainDTOList = trains.stream()
            .map(TrainMapper.INSTANCE::toDto)
            .toList();
    return Response.success(TrainMessages.FIND_BY_CODE_SUCCESS, trainDTOList);
    }

    @Override
    public Response findUnassignedCarriages() {
        List<Carriage> carriages = carriageRepository.findUnassignedCarriages();
        List<CarriageDTO> carriageDTOList = CarriageMapper.INSTANCE.toDtoList(carriages);
        return Response.success(TrainMessages.FIND_UNASSIGNED_SUCCESS, carriageDTOList);
    }

    @Override
    public Response createTrain(CreateTrainDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }
        if (trainRepository.existsByTrainCodeIgnoreCase(dto.getTrainCode())) {
            return Response.error(String.format(TrainMessages.TRAIN_CODE_DUPLICATE, dto.getTrainCode()));
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Train train = trainRepository.createTrain(em, dto.getTrainCode(), dto.getCarriageIds());
                log.info("Train created: trainCode={}, id={}", train.getTrainCode(), train.getId());
                return Response.success(String.format(TrainMessages.CREATE_TRAIN_SUCCESS, dto.getTrainCode()), train.getId());
            });
        } catch (IllegalStateException e) {
            log.warn("Business rule violation during train creation: {}", e.getMessage());
            return Response.error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to create train: trainCode={}", dto.getTrainCode(), e);
            return Response.error(TrainMessages.SYSTEM_ERROR);
        }
    }

    @Override
    public Response createCarriage(CreateCarriageDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                Carriage carriage = carriageRepository.saveCarriageWithSeats(em, dto.getCarriageType());
                log.info("Carriage registered: type={}, id={}", dto.getCarriageType(), carriage.getId());
                return Response.success(TrainMessages.CREATE_CARRIAGE_SUCCESS, CarriageMapper.INSTANCE.toDto(carriage));
            });
        } catch (RuntimeException e) {
            log.error("Failed to register carriage: type={}", dto.getCarriageType(), e);
            return Response.error(TrainMessages.SYSTEM_ERROR);
        }
    }

    @Override
    public Response updateTrainCarriages(UpdateTrainCarriagesDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                long futureScheduleCount = scheduleRepository.countFutureActiveSchedulesByTrainId(em, dto.getTrainId());
                if (futureScheduleCount > 0) {
                    throw new IllegalStateException(TrainMessages.HAS_FUTURE_SCHEDULES);
                }
                trainRepository.updateTrainCarriages(em, dto.getTrainId(), dto.getCarriageIds());
                log.info("Train carriages updated: trainId={}", dto.getTrainId());
                return Response.success(TrainMessages.UPDATE_CARRIAGES_SUCCESS, null);
            });
        } catch (IllegalStateException e) {
            return Response.error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to update train carriages: trainId={}", dto.getTrainId(), e);
            return Response.error(TrainMessages.SYSTEM_ERROR);
        }
    }

    @Override
    public Response updateTrainStatus(UpdateTrainStatusDTO dto) {
        List<String> errors = ValidationUtils.validate(dto);
        if (!errors.isEmpty()) {
            return Response.error(String.join(", ", errors));
        }

        try {
            return AbstractGenericRepositoryImpl.transactional(em -> {
                if (dto.getStatus() == TrainStatus.INACTIVE || dto.getStatus() == TrainStatus.MAINTENANCE) {
                    long futureScheduleCount = scheduleRepository.countFutureActiveSchedulesByTrainId(em, dto.getTrainId());
                    if (futureScheduleCount > 0) {
                        throw new IllegalStateException(TrainMessages.HAS_FUTURE_SCHEDULES);
                    }
                }
                trainRepository.updateTrainStatus(em, dto.getTrainId(), dto.getStatus());
                log.info("Train status updated: trainId={}, status={}", dto.getTrainId(), dto.getStatus());
                return Response.success(TrainMessages.UPDATE_STATUS_SUCCESS, null);
            });
        } catch (IllegalStateException e) {
            return Response.error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to update train status: trainId={}, status={}", dto.getTrainId(), dto.getStatus(), e);
            return Response.error(TrainMessages.SYSTEM_ERROR);
        }
    }
}
